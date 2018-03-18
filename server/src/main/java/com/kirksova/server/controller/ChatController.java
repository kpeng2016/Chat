package com.kirksova.server.controller;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.enumType.TypeOfMessage;
import com.kirksova.server.service.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private static final Logger log = Logger.getLogger(LeaveUser.class);
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat.sendMessageInterlocutor")
    public void sendMessageInterlocutor(@Payload Message chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        Chat chat = new Chat();
        chat.setMessage(chatMessage);
        chat.setMessagingTemplate(messagingTemplate);
        chat.setHeaderAccessor(headerAccessor);
        chat.sendMessage();
    }

    @MessageMapping("/chat.sendMessageUser")
    public void sendMessageUser(@Payload Message chatMessage) {
        MessageUser messageUser = new MessageUser();
        messageUser.setMessage(chatMessage);
        messageUser.setMessagingTemplate(messagingTemplate);
        messageUser.messageUser();
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/0")
    public Message addUser(@Payload Message registerMessage, SimpMessageHeaderAccessor headerAccessor) {
        RegistrationUser user = new RegistrationUser();
        user.setMessageRegistration(registerMessage);
        user.setHeaderAccessor(headerAccessor);
        if (user.register()) {
            //messagingTemplate.convertAndSend("/topic/0",
            return new Message(user.getUser().getId(), "Вы зарегистрированы", TypeOfMessage.CorrectRegistration, null, null);
        } else
            log.info("Not valid registration data");
        //messagingTemplate.convertAndSend("/topic/0",
         return new Message(null, "Неправильные данные, для регистрации введите /register agentВашеИмя " +
                "если вы агент или /register client ВашеИмя если вы клиент", TypeOfMessage.NotValidRegistrationData, null, null);
    }

    @MessageMapping("/chat.search")
    public void searchForAnInterlocutor(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        SearchForAnInterlocutor interlocutor = new SearchForAnInterlocutor();
        interlocutor.setFirstMessage(message);
        interlocutor.setHeaderAccessor(headerAccessor);
        interlocutor.setMessagingTemplate(messagingTemplate);
        interlocutor.searchForAnInterlocutor();
    }

    @MessageMapping("/chat.leave")
    public void leave(@Payload Message messageLeave, SimpMessageHeaderAccessor headerAccessor) {
        LeaveUser leave = new LeaveUser();
        leave.setMessageLeave(messageLeave);
        leave.setHeaderAccessor(headerAccessor);
        leave.setMessagingTemplate(messagingTemplate);
        leave.checkValidLeave();
    }
}
