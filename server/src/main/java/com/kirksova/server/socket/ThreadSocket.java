package com.kirksova.server.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.model.User.TypeOfUser;
import com.kirksova.server.model.UserEntity;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserEntityService;
import com.kirksova.server.service.UserService;
import com.kirksova.server.util.UserEntityConverter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

@Component
public class ThreadSocket implements Runnable {

    @Value("${message.disconnectedOfTheAgent}")
    private String disconnectedOfTheAgent;
    @Value("${message.leave}")
    private String leave;
    @Value("${message.exit}")
    private String exit;

    private static final String REGISTER = "/register (client|agent) .+";
    private static final String SIGN_IN = "/sign in (client|agent) .+";
    private static final Logger log = Logger.getLogger(ThreadSocket.class);
    private List<Message> clientWithoutAgentMessages = new ArrayList<>();
    private Socket userSocket;
    @Autowired
    private UserEntityService userEntityService;
    @Autowired
    private UserService userService;
    @Autowired
    private MessageService messageService;
    private Message userMessage;
    private Message serverMessage;
    private ObjectMapper mapper;
    private PrintWriter outUserWriter;
    private Scanner inUserScanner;
    private User user;
    private User interlocutor;

    @Override
    public void run() {
        try (InputStream userMessageInputStream = userSocket.getInputStream();
            OutputStream userMessageOutputStream = userSocket.getOutputStream()) {
            log.info("Received a new socket connection");
            inUserScanner = new Scanner(userMessageInputStream, "UTF-8");
            outUserWriter = new PrintWriter(new OutputStreamWriter(userMessageOutputStream, "UTF-8"), true);
            mapper = new ObjectMapper();
            userMessage = convertJsonInMessage();
            if (userMessage == null) {
                return;
            }
            while (!checkRegisterAndLoginName()) {
                userMessage = convertJsonInMessage();
                if (userMessage == null) {
                    return;
                }
            }
            if (userMessage == null) {
                return;
            }
            while (inUserScanner.hasNextLine() && userMessage != null) {
                while (interlocutor == null) {
                    searchForAnInterlocutor();
                    if (userMessage == null) {
                        return;
                    }
                }
                while (!leave.equals(userMessage.getText())) {
                    dialogue();
                    if (userMessage == null) {
                        return;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("IOException", e);
        } finally {
            close();
        }
    }

    private void dialogue() throws IOException {
        userMessage = convertJsonInMessage();
        if (userMessage == null) {
            return;
        }
        if (userMessage.getTypeOfMessage() == MessageType.DISCONNECTION_OF_THE_AGENT
            || userMessage.getTypeOfMessage() == MessageType.DISCONNECTION_OF_THE_CLIENT) {
            interlocutor = null;
            return;
        }
        if (leave.equals(userMessage.getText())) {
            serverMessage = messageService.checkValidLeave(user, interlocutor);
            if (user.getUserType() == TypeOfUser.AGENT) {
                outUserWriter.println(mapper.writeValueAsString(serverMessage));
            } else {
                sendMessageInterlocutor(serverMessage);
            }
            if (serverMessage.getTypeOfMessage() == MessageType.LEAVE_CLIENT) {
                outUserWriter
                    .println(mapper.writeValueAsString(messageService.endDialogMessage(user.getId(), interlocutor)));
                sendMessageInterlocutor(messageService.endDialogMessage(interlocutor.getId(), user));
                interlocutor = null;
                return;
            }
        }
        if (userMessage.getTypeOfMessage() == MessageType.MESSAGE_CHAT) {
            sendMessageInterlocutor(userMessage);
        }
    }

    private void searchForAnInterlocutor() throws IOException {
        userMessage = convertJsonInMessage();
        if (userMessage == null) {
            return;
        }
        if (TypeOfUser.AGENT.equals(user.getUserType()) && userMessage.getTo() != null) {
            interlocutor = UserService.getOnlineClients().stream()
                .filter(user1 -> user1.getId().equals(userMessage.getTo())).findFirst().get();
            return;
        }
        if (TypeOfUser.CLIENT.equals(user.getUserType()) && userMessage.getTo() != null) {
            interlocutor = UserService.getOnlineAgents().stream()
                .filter(user1 -> user1.getId().equals(userMessage.getTo())).findFirst().get();
            for (Message clientWithoutAgentMessage : clientWithoutAgentMessages) {
                userMessage = new Message(clientWithoutAgentMessage.getSenderId(),
                    clientWithoutAgentMessage.getText(), MessageType.MESSAGE_CHAT, interlocutor.getId(),
                    interlocutor.getName());
                sendMessageInterlocutor(userMessage);
            }
            clientWithoutAgentMessages.clear();
            return;
        }
        if (user.getUserType() == TypeOfUser.CLIENT && serverMessage.getTo() == null) {
            clientWithoutAgentMessages.add(userMessage);
        }
        serverMessage = userService.searchForAnInterlocutor(userMessage);
        outUserWriter.println(mapper.writeValueAsString(serverMessage));
        if (user.getUserType() == TypeOfUser.CLIENT && serverMessage.getTo() != null) {
            interlocutor = UserService.getOnlineAgents().stream()
                .filter(user1 -> user1.getId().equals(serverMessage.getTo())).findFirst().get();
            serverMessage = userService.getAgentMessageAboutNewDialog(serverMessage);
            sendMessageInterlocutor(serverMessage);
            for (Message clientWithoutAgentMessage : clientWithoutAgentMessages) {
                userMessage = new Message(clientWithoutAgentMessage.getSenderId(),
                    clientWithoutAgentMessage.getText(), MessageType.MESSAGE_CHAT, interlocutor.getId(),
                    interlocutor.getName());
                sendMessageInterlocutor(userMessage);
            }
            clientWithoutAgentMessages.clear();
        }
    }

    private Boolean checkRegisterAndLoginName() throws IOException {
        if (userMessage.getText().matches(REGISTER)) {
            serverMessage = userService.registerUser(userMessage);
            if (serverMessage.getTypeOfMessage() == MessageType.CORRECT_REGISTRATION) {
                outUserWriter.println(mapper.writeValueAsString(serverMessage));
                setRegisterPassword();
                return true;
            } else {
                outUserWriter.println(mapper.writeValueAsString(serverMessage));
                return false;
            }
        }
        if (userMessage.getText().matches(SIGN_IN)) {
            serverMessage = userService.validateUserNameSignIn(userMessage);
            if (serverMessage.getTypeOfMessage() == MessageType.CORRECT_LOGIN_NAME) {
                outUserWriter.println(mapper.writeValueAsString(serverMessage));
                checkLoginPassword();
                return true;
            } else {
                outUserWriter.println(mapper.writeValueAsString(serverMessage));
                return false;
            }
        }
        return false;
    }

    private void setRegisterPassword() throws IOException {
        userMessage = convertJsonInMessage();
        if (userMessage == null) {
            return;
        }
        userService.setPassword(userMessage);
        UserEntity userEntity = userEntityService.getUserById(serverMessage.getSenderId());
        setClientSession(userEntity);
        setAgentSession(userEntity);
        if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
            startDialogue(userMessage);
        }
    }

    private void checkLoginPassword() throws IOException {
        userMessage = convertJsonInMessage();
        if (userMessage == null) {
            return;
        }
        serverMessage = userService.validateUserPasswordSignIn(userMessage);
        while (serverMessage.getTypeOfMessage() != MessageType.CORRECT_LOGIN_PASSWORD) {
            outUserWriter.println(mapper.writeValueAsString(serverMessage));
            userMessage = convertJsonInMessage();
            if (userMessage == null) {
                return;
            }
            serverMessage = userService.validateUserPasswordSignIn(userMessage);
        }
        outUserWriter.println(mapper.writeValueAsString(serverMessage));
        UserEntity userEntity = userEntityService.getUserById(serverMessage.getSenderId());
        setClientSession(userEntity);
        setAgentSession(userEntity);
        if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
            startDialogue(userMessage);
        }
    }

    private void setClientSession(UserEntity userEntity) {
        if (userEntity.getUserType() == User.TypeOfUser.CLIENT) {
            UserEntityConverter userEntityConverter = new UserEntityConverter();
            user = userEntityConverter.convertUserEntityToUser(userEntity);
            user.setUserSocket(userSocket);
            UserService.getOnlineClients().add(user);
        }
    }

    private void setAgentSession(UserEntity userEntity) {
        if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
            UserEntityConverter userEntityConverter = new UserEntityConverter();
            user = userEntityConverter.convertUserEntityToUser(userEntity);
            user.setFreeAgent(true);
            user.setUserSocket(userSocket);
            UserService.getOnlineAgents().add(user);
        }
    }

    private void startDialogue(Message message) throws JsonProcessingException {
        message = userService.deleteClientInQueue(message);
        if (message.getTypeOfMessage() == MessageType.NO_CLIENT_IN_QUEUE) {
            return;
        }
        log.info("Dialogue between agent " + message.getSenderName() + " and client " + user.getName() + " was started");
        outUserWriter.println(mapper.writeValueAsString(message));
        Long messageTo = message.getTo();
        interlocutor = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(messageTo))
            .findFirst().get();
        Message messageForClient = userService.getClientMessageAboutNewDialog(message);
        sendMessageInterlocutor(messageForClient);
        user.iterateClientCountTotal();
        user.setFreeAgent(false);
    }

    private void sendMessageInterlocutor(Message message) {
        if (interlocutor.getUserSocket() != null) {
            messageService.sendMessageToSocket(interlocutor, message);
        } else {
            messageService.sendMessageToWeb(interlocutor, message);
        }
    }

    private Message convertJsonInMessage() throws IOException {
        String line;
        try {
            line = inUserScanner.nextLine();
        } catch (NoSuchElementException e) {
            disconnectUser();
            return null;
        }
        userMessage = mapper.readValue(line, new TypeReference<Message>() {
        });
        if (leave.equals(userMessage.getText()) && interlocutor == null) {
            serverMessage = messageService.checkValidLeave(user, null);
            outUserWriter.println(mapper.writeValueAsString(serverMessage));
            userMessage = convertJsonInMessage();
        }
        if (exit.equals(userMessage.getText())) {
            outUserWriter.println(mapper.writeValueAsString(messageService.endDialogMessage(user.getId(), interlocutor)));
            disconnectUser();
            return null;
        }
        return userMessage;
    }

    private void disconnectUser() {
        if (user != null) {
            if (user.getUserType() == TypeOfUser.AGENT) {
                log.info("Disconnect agent " + user.getName());
                if (interlocutor != null) {
                    serverMessage = new Message(interlocutor.getId(), disconnectedOfTheAgent,
                        MessageType.DISCONNECTION_OF_THE_AGENT);
                    sendMessageInterlocutor(messageService.endDialogMessage(interlocutor.getId(), user));
                    sendMessageInterlocutor(serverMessage);
                }
                UserService.getOnlineAgents().remove(user);
            } else {
                log.info("Disconnect client " + user.getName());
                if (interlocutor != null) {
                    sendMessageInterlocutor(messageService.endDialogMessage(interlocutor.getId(), user));
                    sendMessageInterlocutor(messageService.checkValidLeave(user, interlocutor));
                    interlocutor.setFreeAgent(true);
                }
                UserService.getOnlineClients().remove(user);
            }

        } else {
            log.info("Disconnect no register user");
        }
    }

    private void close() {
        try {
            userSocket.close();
        } catch (Exception e) {
            log.debug("Failed userSocket.close", e);
        }
    }

    public void setUserSocket(Socket userSocket) {
        this.userSocket = userSocket;
    }
}
