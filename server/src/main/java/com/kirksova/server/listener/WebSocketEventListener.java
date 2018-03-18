package com.kirksova.server.listener;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.service.UserService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = Logger.getLogger(WebSocketEventListener.class);
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;
    @Value("${host.topic}")
    private String topic;
    @Value("${user}")
    private String sessionUser;
    @Value("${interlocutor}")
    private String sessionInterlocutor;
    @Value("${message.disconnectedOfTheAgent}")
    private String disconnectedOfTheAgent;
    @Value("${message.disconnectedOfTheClient}")
    private String disconnectedOfTheClient;
    @Value("${message.endDialogue}")
    private String endDialogue;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
        User interlocutor = (User) headerAccessor.getSessionAttributes().get(sessionInterlocutor);
        if (user != null) {
            if (user.getUserType() == User.TypeOfUser.AGENT) {
                log.info("Disconnect agent " + user.getName());
                if (interlocutor != null) {
                    log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName()
                        + " was over");
                    Message chatMessage = new Message(user.getId(), endDialogue, MessageType.END_DIALOGUE);
                    messagingTemplate.convertAndSend(topic + user.getId(), chatMessage);
                    chatMessage = new Message(interlocutor.getId(), endDialogue, MessageType.END_DIALOGUE);
                    messagingTemplate.convertAndSend(topic + interlocutor.getId(), chatMessage);
                    chatMessage = new Message(interlocutor.getId(), disconnectedOfTheAgent,
                        MessageType.DISCONNECTION_OF_THE_AGENT);
                    messagingTemplate.convertAndSend(topic + interlocutor.getId(), chatMessage);
                }
                UserService.getAgents().remove(user);
            } else {
                log.info("Disconnect client " + user.getName());
                if (interlocutor != null) {
                    log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName()
                        + " was over");
                    Message chatMessage = new Message(user.getId(), endDialogue, MessageType.END_DIALOGUE);
                    messagingTemplate.convertAndSend(topic + user.getId(), chatMessage);
                    chatMessage = new Message(interlocutor.getId(), endDialogue, MessageType.END_DIALOGUE);
                    messagingTemplate.convertAndSend(topic + interlocutor.getId(), chatMessage);
                    chatMessage = new Message(interlocutor.getId(), disconnectedOfTheClient,
                        MessageType.DISCONNECTION_OF_THE_CLIENT);
                    messagingTemplate.convertAndSend(topic + interlocutor.getId(), chatMessage);
                    interlocutor.setFreeAgent(true);
                }
                UserService.getClients().remove(user);
            }
        } else {
            log.info("Disconnect no register user");
        }
    }
}
