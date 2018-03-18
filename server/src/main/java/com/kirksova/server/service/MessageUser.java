package com.kirksova.server.service;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.enumType.TypeOfMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

public class MessageUser {

    private Message messageUser;
    private SimpMessageSendingOperations messagingTemplate;

    public void messageUser() {
        Message chatMessage = new Message(messageUser.getSenderId(), messageUser.getText(), TypeOfMessage.YourMessages,
                messageUser.getTo(), messageUser.getNameTo());
        messagingTemplate.convertAndSend("/topic/" + messageUser.getSenderId(), chatMessage);
    }

    public void setMessage(Message messageUser) {
        this.messageUser = messageUser;
    }

    public void setMessagingTemplate(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}
