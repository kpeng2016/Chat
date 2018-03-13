package com.kirksova.server.controller;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.User;
import com.kirksova.server.model.enumType.TypeOfMessage;
import com.kirksova.server.model.enumType.TypeOfUser;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        User user = (User) headerAccessor.getSessionAttributes().get("User");
        if (user != null) {
            if (user.getUserType() == TypeOfUser.agent) {
                log.info("Disconnect agent " + user.getName());
                Long interlocutor = (Long) headerAccessor.getSessionAttributes().get("InterlocutorId");
                if (interlocutor != null) {
                    log.info("test null interlocutor");
                    Message chatMessage = new Message(user.getId(), "Диалог завершен",
                            TypeOfMessage.EndDialogue, null, null);
                    messagingTemplate.convertAndSend("/topic/" + user.getId(), chatMessage);
                    chatMessage = new Message(interlocutor, "Диалог завершен",
                            TypeOfMessage.EndDialogue, null, null);
                    messagingTemplate.convertAndSend("/topic/" + interlocutor, chatMessage);
                    chatMessage = new Message(interlocutor, "Агент вышел из сети, напишите сообщение для " +
                            "соединения с новым агентом или для закрытия приложения, введите /exit", TypeOfMessage.DisconnectionOfTheAgent, null, null);
                    messagingTemplate.convertAndSend("/topic/" + interlocutor, chatMessage);
                }
                user.getAgents().remove(user);
            } else {
                log.info("Disconnect client " + user.getName());
                User interlocutor = (User) headerAccessor.getSessionAttributes().get("Interlocutor");
                if (interlocutor != null) {
                    Message chatMessage = new Message(user.getId(), "Диалог завершен", TypeOfMessage.EndDialogue, null, null);
                    messagingTemplate.convertAndSend("/topic/" + user.getId(), chatMessage);
                    chatMessage = new Message(interlocutor.getId(), "Диалог завершен",
                            TypeOfMessage.EndDialogue, null, null);
                    messagingTemplate.convertAndSend("/topic/" + interlocutor.getId(), chatMessage);
                    chatMessage = new Message(interlocutor.getId(), "Клиент вышел из чата для закрытия приложения используйте /exit, " +
                            "или дождитесь подключения нового клиента", TypeOfMessage.DisconnectionOfTheClient, null, null);
                    messagingTemplate.convertAndSend("/topic/" + interlocutor.getId(), chatMessage);
                    interlocutor.setFreeAgent(true);
                }
                user.getClients().remove(user);
            }
        }
    }
}
