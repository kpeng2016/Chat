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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final String pepper = ".dQUEtby7P35;k\"5EhPB<j.;,9hqvs!(<\"B]=#dBfhnyaN)v>8Z_bs%YJW/u~{w5:4B!s5F>";
    private static final String REGISTER_CLIENT = "/register client .+";
    private static final String REGISTER_AGENT = "/register agent .+";
    private static final String SIGN_IN_AGENT = "/sign in agent .+";
    private static final String SIGN_IN_CLIENT = "/sign in client .+";
    private static final Logger log = Logger.getLogger(UserService.class);
    private static List<User> onlineAgents = Collections.synchronizedList(new ArrayList<User>());
    private static List<User> onlineClients = Collections.synchronizedList(new ArrayList<User>());
    private SimpMessageSendingOperations messagingTemplate;
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
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            log.debug("MessageDigest error", e);
        }
        digest.reset();
        String text = message.getText();
        try {
            digest.update(text.getBytes("utf8"));
            text = String.format("%040x", new BigInteger(1, digest.digest()));
            String salt = UUID.randomUUID().toString();
            String password = text + salt + pepper;
            digest.update(password.getBytes("utf8"));
            password = String.format("%040x", new BigInteger(1, digest.digest()));
            UserEntity userEntity = userEntityService.getUserById(message.getSenderId());
            userEntity.setSalt(salt);
            userEntity.setPassword(password);
            userEntityService.update(userEntity);
        } catch (UnsupportedEncodingException e) {
            log.debug("digest.update error", e);
        }
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
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            log.debug("MessageDigest error", e);
        }
        digest.reset();
        String text = message.getText();
        try {
            digest.update(text.getBytes("utf8"));
            text = String.format("%040x", new BigInteger(1, digest.digest()));
            UserEntity userEntity = userEntityService.getUserById(message.getSenderId());
            String salt = userEntity.getSalt();
            String password = text + salt + pepper;
            digest.update(password.getBytes("utf8"));
            password = String.format("%040x", new BigInteger(1, digest.digest()));
            if (userEntity.getPassword().equals(password)) {
                if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
                    log.info("Login agent " + userEntity.getName());

                } else {
                    log.info("Login client " + userEntity.getName());
                }
                return new Message(message.getSenderId(), correctSignInData, MessageType.CORRECT_LOGIN_PASSWORD);
            }
        } catch (UnsupportedEncodingException e) {
            log.debug("digest.update error", e);
        }
        log.info("Not valid password data");
        return new Message(message.getSenderId(), incorrectLoginPassword, MessageType.INCORRECT_LOGIN_PASSWORD);
    }

    public void startDialogue(Message message, User user) {
        while (user.getMaxClientCount() != user.getClientCountNow()) {
            Message message1 = deleteClientInQueue(message);
            if (message1.getTypeOfMessage() == MessageType.NO_CLIENT_IN_QUEUE) {
                return;
            }
            log.info("Dialogue between agent " + user.getName() + " and client " + message1.getSenderName()
                + " was started");
            messagingTemplate.convertAndSend(topic + message1.getTo(), message1);
            Long messageTo = message1.getSenderId();
            User interlocutor = onlineClients.stream()
                .filter(user1 -> user1.getId().equals(messageTo)).findFirst().get();
            user.getInterlocutorList().put(messageTo, interlocutor);
            interlocutor.getInterlocutorList().put(user.getId(), user);
            Message messageForClient = getClientMessageAboutNewDialog(message1);
            if (interlocutor.getMessagingTemplate() != null) {
                messagingTemplate.convertAndSend(topic + interlocutor.getId(), messageForClient);
            }
            if (interlocutor.getUserSocket() != null) {
                messageService.sendMessageToSocket(interlocutor, messageForClient);
            }
            if (interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null) {
                messageService.sendMessageToRest(interlocutor, messageForClient);
            }
            user.iterateClientCountNow();
            user.iterateClientCountTotal();
            for (int i = 0; i < interlocutor.getMessageWithoutAgent().size(); i++) {
                if (user.getMessagingTemplate() != null) {
                    messagingTemplate
                        .convertAndSend(topic + user.getId(), interlocutor.getMessageWithoutAgent().get(i));
                }
                if (user.getUserSocket() != null) {
                    messageService.sendMessageToSocket(user, interlocutor.getMessageWithoutAgent().get(i));
                }
            }
        }
        user.setFreeAgent(false);
    }

    public Message setMaxClientCount(Message message) {
        Integer maxClientCount = Integer.parseInt(message.getText());
        User user = onlineAgents.stream()
            .filter(user1 -> user1.getId().equals(message.getSenderId())).findFirst().get();
        if (maxClientCount < user.getClientCountNow()) {
            return new Message(message.getSenderId(), incorrectDataMaxCountClients,
                MessageType.INCORRECT_DATA_MAX_COUNT_CLIENTS);
        }
        if (maxClientCount == user.getMaxClientCount()) {
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
            return new Message(client.getId(), connectedClient + client.getName(), MessageType.CONNECTED_CLIENT,
                message.getSenderId(), client.getName());
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
                interlocutor.getInterlocutorList().put(userId, user);
                user.getInterlocutorList().put(interlocutor.getId(), interlocutor);
                log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName()
                    + " was started");
                if (interlocutor.getMaxClientCount() == interlocutor.getClientCountNow()) {
                    interlocutor.setFreeAgent(false);
                }
                return new Message(interlocutor.getId(), connectedAgent + interlocutor.getName(),
                    MessageType.CONNECTED_AGENT, userId, interlocutor.getName());
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
        User user = onlineClients.stream().filter(user1 -> user1.getId().equals(message.getTo()))
            .findFirst().get();
        return new Message(user.getId(), connectedClient + user.getName(), MessageType.CONNECTED_CLIENT,
            message.getSenderId(), user.getName());
    }

    public Message getClientMessageAboutNewDialog(Message message) {
        User user = onlineAgents.stream().filter(user1 -> user1.getId().equals(message.getTo()))
            .findFirst().get();
        return new Message(user.getId(), connectedAgent + user.getName(), MessageType.CONNECTED_AGENT,
            message.getSenderId(), user.getName());
    }

    public void setClientSession(SimpMessageHeaderAccessor headerAccessor, UserEntity userEntity) {
        if (userEntity.getUserType() == User.TypeOfUser.CLIENT) {
            UserEntityConverter userEntityConverter = new UserEntityConverter();
            User user = userEntityConverter.convertUserEntityToUser(userEntity);
            user.setMessagingTemplate(messagingTemplate);
            onlineClients.add(user);
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
            onlineAgents.add(user);
        }
    }

    public static List<User> getOnlineAgents() {
        return onlineAgents;
    }

    public static List<User> getOnlineClients() {
        return onlineClients;
    }

    public void setMessagingTemplate(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}
