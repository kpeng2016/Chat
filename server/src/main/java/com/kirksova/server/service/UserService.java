package com.kirksova.server.service;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final String REGISTER_AGENT = "/register agent .+";
    private static final String REGISTER_CLIENT = "/register client .+";
    private static final String REGISTER_CLIENT1 = "/register client ";
    private static final String REGISTER_AGENT1 = "/register agent ";
    private static List<User> agents = Collections.synchronizedList(new ArrayList<User>());
    private static List<User> clients = Collections.synchronizedList(new ArrayList<User>());
    private static final Logger log = Logger.getLogger(UserService.class);
    private SimpMessageHeaderAccessor headerAccessor;
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


    public User validateAndRegister(Message message) {
        String text = message.getText();
        User user;
        if (text.matches(REGISTER_AGENT)) {
            //добавить пользователя в список агентов, по умолчанию сделать свободным
            String[] name = text.trim().split(REGISTER_AGENT1);
            user = new User(User.TypeOfUser.AGENT, name[1]);
            log.info("Register agent " + name[1]);
            user.setFreeAgent(true);
            agents.add(user);
            if (headerAccessor != null) {
                headerAccessor.getSessionAttributes().put(sessionUser, user);
            }
            return user;
        }
        if (text.matches(REGISTER_CLIENT)) {
            //добавить ползователя в список клиентов
            String[] name = text.trim().split(REGISTER_CLIENT1);
            user = new User(User.TypeOfUser.CLIENT, name[1]);
            log.info("Register client " + name[1]);
            clients.add(user);
            if (headerAccessor != null) {
                headerAccessor.getSessionAttributes().put(sessionUser, user);
            }
            return user;
        }
        log.info("Not valid registration data");
        return null;
    }

    public Message searchForAnInterlocutor(Message message) {
        Long userId = message.getSenderId();
        User user = UserService.getClients().stream().filter(user1 -> user1.getId().equals(userId)).findFirst()
            .orElse(UserService.getAgents().stream().filter(user1 -> user1.getId().equals(userId)).findFirst()
                .orElse(null));
        if (user.getUserType() == User.TypeOfUser.CLIENT) {
            List<User> freeAgents = agents.stream().sorted(Comparator.comparing(User::getUserCount))
                .filter(User::isFreeAgent).collect(Collectors.toList());
            if (freeAgents.size() > 0) {
                User interlocutor = freeAgents.get(0);
                interlocutor.setFreeAgent(false);
                interlocutor.iterateUserCount();
                log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName()
                    + " was started");
                if (user.getUserSocket() == null) {
                    headerAccessor.getSessionAttributes().put(sessionInterlocutor, interlocutor);
                }
                return new Message(userId, connectedAgent + interlocutor.getName(),
                    MessageType.CONNECTED_AGENT, interlocutor.getId(), interlocutor.getName());
            } else {
                if (message.getTypeOfMessage() == MessageType.CALL_SEARCH_FREE_AGENT) {
                    return new Message(userId, wait, MessageType.CALL_SEARCH_FREE_AGENT);
                } else {
                    return new Message(userId, noFreeAgent, MessageType.NO_FREE_AGENT);
                }
            }


        } else {
            if (message.getTo() == null) {
                return new Message(userId, firstMessageAgent,
                    MessageType.FIRST_MESSAGE_AGENT);
            }
            return null;
        }
    }

    public Message getAgentMessageAboutNewDialog(Message message) {
        User user = UserService.getClients().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
            .findFirst().get();
        return new Message(message.getTo(), connectedClient + user.getName(), MessageType.CONNECTED_CLIENT,
            user.getId(), user.getName());
    }

    public static List<User> getAgents() {
        return agents;
    }

    public static List<User> getClients() {
        return clients;
    }

    public void setHeaderAccessor(SimpMessageHeaderAccessor headerAccessor) {
        this.headerAccessor = headerAccessor;
    }
}
