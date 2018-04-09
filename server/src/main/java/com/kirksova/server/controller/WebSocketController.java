package com.kirksova.server.controller;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.model.UserEntity;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserEntityService;
import com.kirksova.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;
    private final UserEntityService userEntityService;
    @Value("${host.topic}")
    private String topic;
    @Value("${user}")
    private String sessionUser;
    @Value("${interlocutor}")
    private String sessionInterlocutor;

    @Autowired
    public WebSocketController(SimpMessageSendingOperations messagingTemplate, MessageService messageService,
        UserService userService, UserEntityService userEntityService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.userService = userService;
        this.userEntityService = userEntityService;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/start")
    public Message addUser(@Payload Message registerMessage) {
        return userService.registerUser(registerMessage);
    }

    @MessageMapping("/chat.signInUser")
    @SendTo("/topic/start")
    public Message loginUser(@Payload Message registerMessage) {
        return userService.validateUserNameSignIn(registerMessage);
    }

    @MessageMapping("/chat.setPassword")
    public void setPassword(@Payload Message registerMessage, SimpMessageHeaderAccessor headerAccessor) {
        userService.setPassword(registerMessage);
        UserEntity userEntity = userEntityService.getUserById(registerMessage.getSenderId());
        userService.setMessagingTemplate(messagingTemplate);
        userService.setClientSession(headerAccessor, userEntity);
        userService.setAgentSession(headerAccessor, userEntity);
        if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
            User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
            userService.startDialogue(registerMessage, user);
        }
    }

    @MessageMapping("/chat.checkPassword")
    public void checkPassword(@Payload Message registerMessage, SimpMessageHeaderAccessor headerAccessor) {
        registerMessage = userService.validateUserPasswordSignIn(registerMessage);
        messagingTemplate.convertAndSend(topic + registerMessage.getSenderId(),
            registerMessage);
        if (registerMessage.getTypeOfMessage() == MessageType.CORRECT_LOGIN_PASSWORD) {
            UserEntity userEntity = userEntityService.getUserById(registerMessage.getSenderId());
            userService.setMessagingTemplate(messagingTemplate);
            userService.setClientSession(headerAccessor, userEntity);
            userService.setAgentSession(headerAccessor, userEntity);
            if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
                User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
                userService.startDialogue(registerMessage, user);
            }
        }
    }

    @MessageMapping("/chat.setMaxClients")
    public void setMaxClient(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        userService.setHeaderAccessor(headerAccessor);
        message = userService.setMaxClientCount(message);
        messagingTemplate.convertAndSend(topic + message.getSenderId(), message);
        if (message.getTypeOfMessage() == MessageType.CORRECT_DATA_MAX_COUNT_CLIENTS) {
            userService.setMessagingTemplate(messagingTemplate);
            User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
            userService.startDialogue(message, user);
        }
    }

    @MessageMapping("/chat.sendMessageInterlocutor")
    public void sendMessageInterlocutor(@Payload Message chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
        User interlocutor = user.getClientsAgent().get(chatMessage.getTo());
        if (interlocutor == null) {
            interlocutor = (User) headerAccessor.getSessionAttributes().get(sessionInterlocutor);
            if (interlocutor == null) {
                interlocutor = UserService.getOnlineAgents().stream()
                    .filter(user1 -> user1.getId().equals(chatMessage.getTo())).findFirst().get();
                headerAccessor.getSessionAttributes().put(sessionInterlocutor, interlocutor);
            }
        }
        chatMessage.setSenderName(user.getName());
        if (interlocutor.getMessagingTemplate() != null) {
            messagingTemplate.convertAndSend(topic + interlocutor.getId(), chatMessage);
        }
        if (interlocutor.getUserSocket() != null) {
            messageService.sendMessageToSocket(interlocutor, chatMessage);
        }
        if (interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null) {
            interlocutor.getMessagesForInterlocutor().add(chatMessage);
        }
    }

    @MessageMapping("/chat.search")
    public void searchForAnInterlocutor(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        userService.setHeaderAccessor(headerAccessor);
        message = userService.searchForAnInterlocutor(message);
        User interlocutor = (User) headerAccessor.getSessionAttributes().get(sessionInterlocutor);
        if (interlocutor != null) {
            messagingTemplate.convertAndSend(topic + message.getTo(), message);
            message = userService.getAgentMessageAboutNewDialog(message);
            if (interlocutor.getMessagingTemplate() != null) {
                messagingTemplate.convertAndSend(topic + interlocutor.getId(), message);
            }
            if (interlocutor.getUserSocket() != null) {
                messageService.sendMessageToSocket(interlocutor, message);
            }
            if (interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null) {
                interlocutor.getMessagesForInterlocutor().add(message);
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
        if (messageLeave.getTo() != null) {
            Message endDialog = messageService.endDialogMessage(user.getId(), interlocutor);
            messagingTemplate.convertAndSend(topic + messageLeave.getSenderId(), endDialog);
            endDialog = messageService.endDialogMessage(messageLeave.getTo(), user);
            if (interlocutor.getMessagingTemplate() != null) {
                messagingTemplate.convertAndSend(topic + messageLeave.getTo(), endDialog);
                messagingTemplate.convertAndSend(topic + messageLeave.getTo(), message);
            }
            if (interlocutor.getUserSocket() != null) {
                messageService.sendMessageToSocket(interlocutor, endDialog);
                messageService.sendMessageToSocket(interlocutor, message);
            }
            if (interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null) {
                interlocutor.getMessagesForInterlocutor().add(endDialog);
                interlocutor.getMessagesForInterlocutor().add(message);
            }
            userService.setMessagingTemplate(messagingTemplate);
            userService.startDialogue(message, interlocutor);
            headerAccessor.getSessionAttributes().remove(sessionInterlocutor);
        } else {
            messagingTemplate.convertAndSend(topic + user.getId(), message);
        }
    }


}