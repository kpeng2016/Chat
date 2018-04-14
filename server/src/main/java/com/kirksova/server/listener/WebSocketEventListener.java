package com.kirksova.server.listener;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    private static final Logger log = Logger.getLogger(WebSocketEventListener.class);
    @Autowired
    private MessageService messageService;
    @Autowired
    private UserService userService;
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;
    @Value("${host.topic}")
    private String topic;
    @Value("${user}")
    private String sessionUser;
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
        User interlocutor = null;
        if (user != null) {
            if (user.getUserType() == User.TypeOfUser.AGENT) {
                log.info("Disconnect agent " + user.getName());
                for (Map.Entry<Long, User> entry: user.getInterlocutorList().entrySet()){
                    interlocutor = entry.getValue();
                    log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName()
                        + " was over");
                    messagingTemplate.convertAndSend(topic + user.getId(), messageService.endDialogMessage(user.getId(), interlocutor));
                    Message chatMessage = messageService.endDialogMessage(interlocutor.getId(), user);
                    Message disconnectedMessage = new Message(interlocutor.getId(), disconnectedOfTheAgent,
                        MessageType.DISCONNECTION_OF_THE_AGENT);
                    if(interlocutor.getMessagingTemplate() != null) {

                        messagingTemplate.convertAndSend(topic + interlocutor.getId(), chatMessage);
                        messagingTemplate.convertAndSend(topic + interlocutor.getId(), disconnectedMessage);
                    }
                    if(interlocutor.getUserSocket() != null){
                        messageService.sendMessageToSocket(interlocutor, chatMessage);
                        messageService.sendMessageToSocket(interlocutor, disconnectedMessage);
                    }
                    if(interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null){
                        messageService.sendMessageToRest(interlocutor, chatMessage);
                        messageService.sendMessageToRest(interlocutor, disconnectedMessage);
                    }
                }
                user.getInterlocutorList().clear();
                UserService.getOnlineAgents().remove(user);
            } else {
                log.info("Disconnect client " + user.getName());
                if(user.getInterlocutorList().values().iterator().hasNext()) {
                    interlocutor = user.getInterlocutorList().values().iterator().next();
                }
                if (interlocutor != null) {
                    log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName()
                        + " was over");
                    messagingTemplate.convertAndSend(topic + user.getId(), messageService.endDialogMessage(user.getId(), interlocutor));
                    Message chatMessage = messageService.endDialogMessage(interlocutor.getId(), user);
                    Message disconnectedMessage = new Message(interlocutor.getId(), disconnectedOfTheClient,
                        MessageType.DISCONNECTION_OF_THE_CLIENT);
                    if(interlocutor.getMessagingTemplate() != null) {
                        messagingTemplate.convertAndSend(topic + interlocutor.getId(), chatMessage);
                        messagingTemplate.convertAndSend(topic + interlocutor.getId(), disconnectedMessage);
                    }
                    if(interlocutor.getUserSocket() != null){
                        messageService.sendMessageToSocket(interlocutor, chatMessage);
                        messageService.sendMessageToSocket(interlocutor, disconnectedMessage);
                    }
                    if(interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null){
                        messageService.sendMessageToRest(interlocutor, chatMessage);
                        messageService.sendMessageToRest(interlocutor, disconnectedMessage);
                    }
                    interlocutor.deleteClientCountNow();
                    interlocutor.getInterlocutorList().remove(user.getId(), user);
                    userService.setMessagingTemplate(messagingTemplate);
                    userService.startDialogue(chatMessage, interlocutor);
                }
                UserService.getOnlineClients().remove(user);
            }
        } else {
            log.info("Disconnect no register user");
        }
    }
}
