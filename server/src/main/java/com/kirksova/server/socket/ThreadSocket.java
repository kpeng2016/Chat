package com.kirksova.server.socket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.model.User.TypeOfUser;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:configuration.properties")
public class ThreadSocket implements Runnable {

    @Value("${message.correctRegistration}")
    private String correctRegistration;
    @Value("${message.notValidRegistrationData}")
    private String notValidRegistrationData;
    @Value("${message.disconnectedOfTheAgent}")
    private String disconnectedOfTheAgent;
    @Value("${message.leave}")
    private String leave;
    @Value("${message.exit}")
    private String exit;

    private static final Logger log = Logger.getLogger(ThreadSocket.class);
    private List<Message> clientWithoutAgentMessages = new ArrayList<>();
    private Socket userSocket;
    private UserService userService;
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
            userService = new UserService();
            messageService = new MessageService();
            mapper = new ObjectMapper();
            userMessage = convertJsonInMessage();
            if (userMessage == null) {
                return;
            }
            register();
            while (inUserScanner.hasNextLine() && userMessage != null) {
                while (interlocutor == null) {
                    userMessage = convertJsonInMessage();
                    if (userMessage == null) {
                        return;
                    }
                    if (TypeOfUser.AGENT.equals(user.getUserType()) && userMessage.getTo() != null) {
                        interlocutor = UserService.getClients().stream()
                            .filter(user1 -> user1.getId().equals(userMessage.getTo())).findFirst().get();
                        continue;
                    }
                    if (user.getUserType() == TypeOfUser.CLIENT && serverMessage.getTo() == null
                        && userMessage.getTypeOfMessage() != MessageType.CALL_SEARCH_FREE_AGENT) {
                        clientWithoutAgentMessages.add(userMessage);
                    }
                    serverMessage = userService.searchForAnInterlocutor(userMessage);
                    outUserWriter.println(mapper.writeValueAsString(serverMessage));
                    if (user.getUserType() == TypeOfUser.CLIENT && serverMessage.getTo() != null) {
                        interlocutor = UserService.getAgents().stream()
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
                while (!leave.equals(userMessage.getText())) {
                    userMessage = convertJsonInMessage();
                    if (userMessage == null) {
                        return;
                    }
                    if (userMessage.getTypeOfMessage() == MessageType.DISCONNECTION_OF_THE_AGENT
                        || userMessage.getTypeOfMessage() == MessageType.DISCONNECTION_OF_THE_CLIENT) {
                        interlocutor = null;
                        break;
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
                                .println(mapper.writeValueAsString(messageService.endDialogMessage(user.getId())));
                            sendMessageInterlocutor(messageService.endDialogMessage(interlocutor.getId()));
                            interlocutor = null;
                            break;
                        }
                    }
                    if (userMessage.getTypeOfMessage() == MessageType.MESSAGE_CHAT) {
                        sendMessageInterlocutor(userMessage);
                    }
                }
            }
        } catch (IOException e) {
            log.debug("IOException", e);
        } finally {
            close();
        }
    }

    private void register() throws IOException {
        user = userService.validateAndRegister(userMessage);
        while (user == null) {
            serverMessage = new Message(null, notValidRegistrationData, MessageType.NOT_VALID_REGISTRATION_DATA);
            outUserWriter.println(mapper.writeValueAsString(serverMessage));
            user = userService.validateAndRegister(userMessage);
        }
        serverMessage = new Message(user.getId(), correctRegistration, MessageType.CORRECT_REGISTRATION);
        outUserWriter.println(mapper.writeValueAsString(serverMessage));
        user.setUserSocket(userSocket);
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
            outUserWriter.println(mapper.writeValueAsString(messageService.endDialogMessage(user.getId())));
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
                    sendMessageInterlocutor(messageService.endDialogMessage(interlocutor.getId()));
                    sendMessageInterlocutor(serverMessage);
                }
                UserService.getAgents().remove(user);
            } else {
                log.info("Disconnect client " + user.getName());
                if (interlocutor != null) {
                    sendMessageInterlocutor(messageService.endDialogMessage(interlocutor.getId()));
                    sendMessageInterlocutor(messageService.checkValidLeave(user, interlocutor));
                    interlocutor.setFreeAgent(true);
                }
                UserService.getClients().remove(user);
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
