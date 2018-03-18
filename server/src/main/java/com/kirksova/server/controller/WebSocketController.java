package com.kirksova.server.controller;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private static final Logger log = Logger.getLogger(WebSocketController.class);
    private final SimpMessageSendingOperations messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;
    @Value("${host.topic}")
    private String topic;
    @Value("${user}")
    private String sessionUser;
    @Value("${interlocutor}")
    private String sessionInterlocutor;
    @Value("${message.correctRegistration}")
    private String correctRegistration;
    @Value("${message.notValidRegistrationData}")
    private String notValidRegistrationData;


    @Autowired
    public WebSocketController(SimpMessageSendingOperations messagingTemplate, MessageService messageService,
        UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.userService = userService;
    }

    @MessageMapping("/chat.sendMessageInterlocutor")
    public void sendMessageInterlocutor(@Payload Message chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
        User interlocutor = (User) headerAccessor.getSessionAttributes().get(sessionInterlocutor);
        if (interlocutor == null) {
            interlocutor = UserService.getClients().stream().filter(user1 -> user1.getId().equals(chatMessage.getTo()))
                .findFirst().get();
            headerAccessor.getSessionAttributes().put(sessionInterlocutor, interlocutor);
        }
        if (interlocutor.getUserSocket() == null) {
            chatMessage.setNameTo(user.getName());
            messagingTemplate.convertAndSend(topic + chatMessage.getTo(), chatMessage);
        } else {
            chatMessage.setNameTo(user.getName());
            messageService.sendMessageToSocket(interlocutor, chatMessage);
        }
    }

    @MessageMapping("/chat.sendMessageUser")
    public void sendMessageUser(@Payload Message chatMessage) {
        chatMessage.setTypeOfMessage(MessageType.YOUR_MESSAGES);
        messagingTemplate.convertAndSend(topic + chatMessage.getSenderId(), chatMessage);
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/0")
    public Message addUser(@Payload Message registerMessage, SimpMessageHeaderAccessor headerAccessor) {
        userService.setHeaderAccessor(headerAccessor);
        User user = userService.validateAndRegister(registerMessage);
        if (user != null) {
            user.setMessagingTemplate(messagingTemplate);
            return new Message(user.getId(), correctRegistration, MessageType.CORRECT_REGISTRATION);
        } else {
            log.info("Not valid registration data");
        }
        return new Message(null, notValidRegistrationData, MessageType.NOT_VALID_REGISTRATION_DATA);
    }

    @MessageMapping("/chat.search")
    public void searchForAnInterlocutor(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        userService.setHeaderAccessor(headerAccessor);
        message = userService.searchForAnInterlocutor(message);
        User interlocutor = (User) headerAccessor.getSessionAttributes().get(sessionInterlocutor);
        if (interlocutor != null) {
            messagingTemplate.convertAndSend(topic + message.getSenderId(), message);
            message = userService.getAgentMessageAboutNewDialog(message);
            if (interlocutor.getUserSocket() == null) {
                messagingTemplate.convertAndSend(topic + interlocutor.getId(), message);
            } else {
                messageService.sendMessageToSocket(interlocutor, message);
            }
        } else {
            messagingTemplate.convertAndSend(topic + message.getSenderId(), message);
        }
    }

    @MessageMapping("/chat.leave")
    public void leave(@Payload Message messageLeave, SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
        User interlocutor = (User) headerAccessor.getSessionAttributes().get(sessionInterlocutor);
        Message message = messageService.checkValidLeave(user, interlocutor);
        if (messageLeave.getTo() == null) {
            messagingTemplate.convertAndSend(topic + user.getId(), message);
        } else {
            if (user.getUserType() == User.TypeOfUser.CLIENT) {
                Message endDialog = messageService.endDialogMessage(user.getId());
                messagingTemplate.convertAndSend(topic + messageLeave.getSenderId(), endDialog);
                endDialog = messageService.endDialogMessage(messageLeave.getTo());
                if (interlocutor.getUserSocket() == null) {
                    messagingTemplate.convertAndSend(topic + messageLeave.getTo(), endDialog);
                    messagingTemplate.convertAndSend(topic + messageLeave.getTo(), message);
                } else {
                    messageService.sendMessageToSocket(interlocutor, endDialog);
                    messageService.sendMessageToSocket(interlocutor, messageLeave);
                }
            } else {
                messagingTemplate.convertAndSend(topic + messageLeave.getSenderId(), message);
            }
        }
    }
}
