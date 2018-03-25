package com.kirksova.server.controller;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.model.UserEntity;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserEntityService;
import com.kirksova.server.service.UserService;
import com.kirksova.server.util.UserEntityConverter;
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
        setClientSession(headerAccessor, userEntity);
        setAgentSession(headerAccessor, userEntity);
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
            setClientSession(headerAccessor, userEntity);
            setAgentSession(headerAccessor, userEntity);
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
            User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
            userService.startDialogue(message, user);
        }
    }

    @MessageMapping("/chat.sendMessageInterlocutor")
    public void sendMessageInterlocutor(@Payload Message chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get(sessionUser);
        User interlocutor;
        if (user.getUserType() == User.TypeOfUser.CLIENT) {
            interlocutor = (User) headerAccessor.getSessionAttributes().get(sessionInterlocutor);
            if (interlocutor == null) {
                interlocutor = UserService.getOnlineAgents().stream()
                    .filter(user1 -> user1.getId().equals(chatMessage.getTo())).findFirst().get();
                headerAccessor.getSessionAttributes().put(sessionInterlocutor, interlocutor);
            }
        } else {
            interlocutor = (User) headerAccessor.getSessionAttributes().get(sessionInterlocutor + chatMessage.getTo());
            if (interlocutor == null) {
                interlocutor = UserService.getOnlineClients().stream()
                    .filter(user1 -> user1.getId().equals(chatMessage.getTo())).findFirst().get();
                headerAccessor.getSessionAttributes().put(sessionInterlocutor + chatMessage.getTo(), interlocutor);
            }
        }
        if (interlocutor.getUserSocket() == null) {
            chatMessage.setNameTo(user.getName());
            messagingTemplate.convertAndSend(topic + chatMessage.getTo(), chatMessage);
        } else {
            chatMessage.setNameTo(user.getName());
            messageService.sendMessageToSocket(interlocutor, chatMessage);
        }
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
        if (messageLeave.getTo() != null) {
            Message endDialog = messageService.endDialogMessage(user.getId());
            messagingTemplate.convertAndSend(topic + messageLeave.getSenderId(), endDialog);
            endDialog = messageService.endDialogMessage(messageLeave.getTo());
            interlocutor.deleteClientCountNow();
            if (interlocutor.getUserSocket() == null) {
                messagingTemplate.convertAndSend(topic + messageLeave.getTo(), endDialog);
                messagingTemplate.convertAndSend(topic + messageLeave.getTo(), message);
            } else {
                messageService.sendMessageToSocket(interlocutor, endDialog);
                messageService.sendMessageToSocket(interlocutor, messageLeave);
            }
            userService.startDialogue(message, interlocutor);
        } else {
            messagingTemplate.convertAndSend(topic + user.getId(), message);
        }
    }

    private void setClientSession(SimpMessageHeaderAccessor headerAccessor, UserEntity userEntity) {
        if (userEntity.getUserType() == User.TypeOfUser.CLIENT) {
            UserEntityConverter userEntityConverter = new UserEntityConverter();
            User user = userEntityConverter.convertUserEntityToUser(userEntity);
            user.setMessagingTemplate(messagingTemplate);
            UserService.getOnlineClients().add(user);
            headerAccessor.getSessionAttributes().put(sessionUser, user);
        }
    }

    private void setAgentSession(SimpMessageHeaderAccessor headerAccessor, UserEntity userEntity) {
        if (userEntity.getUserType() == User.TypeOfUser.AGENT) {
            UserEntityConverter userEntityConverter = new UserEntityConverter();
            User user = userEntityConverter.convertUserEntityToUser(userEntity);
            if (user.getMaxClientCount() == 0) {
                user.setMaxClientCount(1);
            }
            user.setFreeAgent(true);
            headerAccessor.getSessionAttributes().put(sessionUser, user);
            userService.setMessagingTemplate(messagingTemplate);
            user.setMessagingTemplate(messagingTemplate);
            UserService.getOnlineAgents().add(user);
        }
    }
}

