package com.kirksova.server.service;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.model.UserEntity;
import com.kirksova.server.queue.ClientQueue;
import com.kirksova.server.util.UserEntityConverter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final String REGISTER_CLIENT = "/register client .+";
    private static final String REGISTER_AGENT = "/register agent .+";
    private static final String SIGN_IN_AGENT = "/sign in agent .+";
    private static final String SIGN_IN_CLIENT = "/sign in client .+";
    private static final Logger log = Logger.getLogger(UserService.class);
    private static List<User> onlineAgents = Collections.synchronizedList(new ArrayList<User>());
    private static List<User> onlineClients = Collections.synchronizedList(new ArrayList<User>());
    private SimpMessageSendingOperations messagingTemplate;
    private SimpMessageHeaderAccessor headerAccessor;
    @Autowired
    private MessageService messageService;
    @Autowired
    private UserEntityService userEntityService;
    @Autowired
    private static ClientQueue clientQueue = new ClientQueue();
    @Value("${host.topic}")
    private String topic;
    @Value("${user}")
    private String sessionUser;
    @Value("${interlocutor}")
    private String sessionInterlocutor;
    @Value("${message.noFreeAgent}")
    private String noFreeAgent;
    @Value("${message.wait}")
    private String wait;
    @Value("${message.firstMessageAgent}")
    private String firstMessageAgent;
    @Value("${message.connectedAgent}")
    private String connectedAgent;
    @Value("${message.connectedClient}")
    private String connectedClient;
    @Value("${message.noClientInQueue}")
    private String noClientInQueue;
    @Value("${message.correctRegistration}")
    private String correctRegistration;
    @Value("${message.incorrectRegistrationData}")
    private String incorrectRegistrationData;
    @Value("${message.incorrectLoginName}")
    private String incorrectLoginName;
    @Value("${message.correctSignInData}")
    private String correctSignInData;
    @Value("${message.incorrectLoginPassword}")
    private String incorrectLoginPassword;
    @Value("${message.incorrectDataMaxCountClients}")
    private String incorrectDataMaxCountClients;
    @Value("${message.correctDataMaxCountClients}")
    private String correctDataMaxCountClients;
    @Value("${message.notUpdateDataMaxCountClients}")
    private String notUpdateDataMaxCountClients;

    public Message registerUser(Message message) {
        String text = message.getText();
        if (text.matches(REGISTER_AGENT)) {
            String name = text.substring(REGISTER_AGENT.length() - 2);
            if (!userEntityService.existsUserWithName(name)) {
                UserEntity userEntity = new UserEntity();
                userEntity.setUserType(User.TypeOfUser.AGENT);
                userEntity.setName(name);
                log.info("Register agent " + name);
                userEntity = userEntityService.create(userEntity);
                return new Message(userEntity.getId(), correctRegistration, MessageType.CORRECT_REGISTRATION);
            }
        }
        if (text.matches(REGISTER_CLIENT)) {
            String name = text.substring(REGISTER_CLIENT.length() - 2);
            if (!userEntityService.existsUserWithName(name)) {
                UserEntity userEntity = new UserEntity();
                userEntity.setUserType(User.TypeOfUser.CLIENT);
                userEntity.setName(name);
                log.info("Register client " + name);
                userEntity = userEntityService.create(userEntity);
                return new Message(userEntity.getId(), correctRegistration, MessageType.CORRECT_REGISTRATION);
            }
        }
        log.info("Not valid registration data");
        return new Message(null, incorrectRegistrationData, MessageType.INCORRECT_REGISTRATION_DATA);
    }

    public void setPassword(Message message) {
        String text = message.getText();
        UserEntity userEntity = userEntityService.getUserById(message.getSenderId());
        userEntity.setPassword(text);
        userEntityService.update(userEntity);
    }

    public Message validateUserNameSignIn(Message message) {
        String text = message.getText();
        Message response;
        if ((response = getMessage(text, SIGN_IN_AGENT)) != null) {
            return response;
        }
        if ((response = getMessage(text, SIGN_IN_CLIENT)) != null) {
            return response;
        }
        log.info("Not valid name data");
        return new Message(null, incorrectLoginName, MessageType.INCORRECT_LOGIN_NAME);
    }

    private Message getMessage(String text, String regexString) {
        if (text.matches(regexString)) {
            String name = text.substring(regexString.length() - 2);
            User.TypeOfUser role;
            if (text.charAt(9) == 'c') {
                role = User.TypeOfUser.CLIENT;
            } else {
                role = User.TypeOfUser.AGENT;
            }
            if (userEntityService.existsUserWithName(name)) {
                UserEntity userEntity = userEntityService.getUserByName(name);
                if (userEntity.getUserType() == role) {
                    return new Message(userEntity.getId(), correctRegistration, MessageType.CORRECT_LOGIN_NAME);
                }
            }
        }
        return null;
    }

    public Message validateUserPasswordSignIn(Message message) {
        String text = message.getText();
        UserEntity userEntity = userEntityService.getUserById(message.getSenderId());
        if (userEntity.getPassword().equals(text)) {
            if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
                log.info("Login agent " + userEntity.getName());

            } else {
                log.info("Login client " + userEntity.getName());
            }
            return new Message(message.getSenderId(), correctSignInData, MessageType.CORRECT_LOGIN_PASSWORD);
        }
        log.info("Not valid password data");
        return new Message(message.getSenderId(), incorrectLoginPassword, MessageType.INCORRECT_LOGIN_PASSWORD);
    }

    public void startDialogue(Message message, User user) {
        while (user.getMaxClientCount() != user.getClientCountNow()) {
            message = deleteClientInQueue(message);
            if (message.getTypeOfMessage() == MessageType.NO_CLIENT_IN_QUEUE) {
                return;
            }
            log.info("Dialogue between agent " + message.getNameTo() + " and client " + user.getName()
                + " was started");
            messagingTemplate.convertAndSend(topic + message.getSenderId(), message);
            Long messageTo = message.getTo();
            User interlocutor = onlineClients.stream()
                .filter(user1 -> user1.getId().equals(messageTo)).findFirst().get();
            user.getClientsAgent().put(messageTo, interlocutor);
            Message messageForClient = getClientMessageAboutNewDialog(message);
            if (interlocutor.getMessagingTemplate() != null) {
                messagingTemplate.convertAndSend(topic + messageForClient.getSenderId(), messageForClient);
            } else {
                messageService.sendMessageToSocket(interlocutor, messageForClient);
            }
            user.iterateClientCountNow();
            user.iterateClientCountTotal();
        }
        user.setFreeAgent(false);
    }

    public Message setMaxClientCount(Message message) {
        Integer maxClientCount = Integer.parseInt(message.getText());
        User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
        if (maxClientCount < user.getClientCountNow()) {
            return new Message(message.getSenderId(), incorrectDataMaxCountClients,
                MessageType.INCORRECT_DATA_MAX_COUNT_CLIENTS);
        }
        if(maxClientCount == user.getMaxClientCount()){
            return new Message(message.getSenderId(), notUpdateDataMaxCountClients,
                MessageType.INCORRECT_DATA_MAX_COUNT_CLIENTS);
        }
        user.setMaxClientCount(maxClientCount);
        user.setFreeAgent(true);
        UserEntity userEntity = userEntityService.getUserById(message.getSenderId());
        userEntity.setMaxClientCount(maxClientCount);
        userEntityService.update(userEntity);
        return new Message(message.getSenderId(), correctDataMaxCountClients,
            MessageType.CORRECT_DATA_MAX_COUNT_CLIENTS);
    }

    private void putClientInQueue(User user) {
        for (int i = 0; i < clientQueue.size(); i++) {
            if (user == clientQueue.get(i)) {
                return;
            }
        }
        clientQueue.push(user);
    }

    public List<User> getClientsInQueue() {
        List<User> clients = new ArrayList<>();
        for (int i = 0; i < clientQueue.size(); i++) {
            clients.add(clientQueue.get(i));
        }
        return clients;
    }

    public Message deleteClientInQueue(Message message) {
        User client = clientQueue.pull();
        if (client != null) {
            return new Message(message.getSenderId(), connectedClient + client.getName(), MessageType.CONNECTED_CLIENT,
                client.getId(), client.getName());
        }
        return new Message(message.getSenderId(), noClientInQueue, MessageType.NO_CLIENT_IN_QUEUE);
    }

    public Message searchForAnInterlocutor(Message message) {
        Long userId = message.getSenderId();
        User user = onlineClients.stream().filter(user1 -> user1.getId().equals(userId)).findFirst()
            .orElse(onlineAgents.stream().filter(user1 -> user1.getId().equals(userId)).findFirst()
                .orElse(null));
        if (user.getUserType() == User.TypeOfUser.CLIENT) {
            List<User> freeAgents = onlineAgents.stream().sorted(Comparator.comparing(User::getClientCountTotal))
                .filter(User::isFreeAgent).collect(Collectors.toList());
            if (freeAgents.size() > 0) {
                User interlocutor = freeAgents.get(0);
                interlocutor.iterateClientCountTotal();
                interlocutor.iterateClientCountNow();
                interlocutor.getClientsAgent().put(userId, user);
                log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName()
                    + " was started");
                if (interlocutor.getMaxClientCount() == interlocutor.getClientCountNow()) {
                    interlocutor.setFreeAgent(false);
                }
                if (user.getUserSocket() == null) {
                    headerAccessor.getSessionAttributes().put(sessionInterlocutor, interlocutor);
                }
                return new Message(userId, connectedAgent + interlocutor.getName(),
                    MessageType.CONNECTED_AGENT, interlocutor.getId(), interlocutor.getName());
            } else {
                putClientInQueue(user);
                return new Message(userId, noFreeAgent, MessageType.NO_FREE_AGENT);
            }
        } else {
            if (message.getTo() == null) {
                return new Message(userId, firstMessageAgent, MessageType.FIRST_MESSAGE_AGENT);
            }
            return null;
        }
    }

    public Message getAgentMessageAboutNewDialog(Message message) {
        User user = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
            .findFirst().get();
        return new Message(message.getTo(), connectedClient + user.getName(), MessageType.CONNECTED_CLIENT,
            user.getId(), user.getName());
    }

    public Message getClientMessageAboutNewDialog(Message message) {
        User user = UserService.getOnlineAgents().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
            .findFirst().get();
        return new Message(message.getTo(), connectedAgent + user.getName(), MessageType.CONNECTED_AGENT,
            user.getId(), user.getName());
    }

    public void setClientSession(SimpMessageHeaderAccessor headerAccessor, UserEntity userEntity) {
        if (userEntity.getUserType() == User.TypeOfUser.CLIENT) {
            UserEntityConverter userEntityConverter = new UserEntityConverter();
            User user = userEntityConverter.convertUserEntityToUser(userEntity);
            user.setMessagingTemplate(messagingTemplate);
            UserService.getOnlineClients().add(user);
            headerAccessor.getSessionAttributes().put(sessionUser, user);
        }
    }

    public void setAgentSession(SimpMessageHeaderAccessor headerAccessor, UserEntity userEntity) {
        if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
            UserEntityConverter userEntityConverter = new UserEntityConverter();
            User user = userEntityConverter.convertUserEntityToUser(userEntity);
            if (user.getMaxClientCount() == 0) {
                user.setMaxClientCount(1);
            }
            user.setFreeAgent(true);
            user.setMessagingTemplate(messagingTemplate);
            headerAccessor.getSessionAttributes().put(sessionUser, user);
            UserService.getOnlineAgents().add(user);
        }
    }

    public static List<User> getOnlineAgents() {
        return onlineAgents;
    }

    public static List<User> getOnlineClients() {
        return onlineClients;
    }

    public void setHeaderAccessor(SimpMessageHeaderAccessor headerAccessor) {
        this.headerAccessor = headerAccessor;
    }

    public void setMessagingTemplate(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}
