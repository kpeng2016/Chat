package com.kirksova.server.service;

import com.kirksova.server.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

public class Chat {

    private Message message;
    private SimpMessageHeaderAccessor headerAccessor;
    private SimpMessageSendingOperations messagingTemplate;

    public void sendMessage() {
        headerAccessor.getSessionAttributes().put("InterlocutorId", message.getTo());
        messagingTemplate.convertAndSend("/topic/" + message.getTo(), message);
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setHeaderAccessor(SimpMessageHeaderAccessor headerAccessor) {
        this.headerAccessor = headerAccessor;
    }

    public void setMessagingTemplate(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}
