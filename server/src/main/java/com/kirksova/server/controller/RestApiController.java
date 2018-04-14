package com.kirksova.server.controller;

import com.kirksova.server.error.CustomErrorType;
import com.kirksova.server.listener.WebSocketEventListener;
import com.kirksova.server.model.*;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserEntityService;
import com.kirksova.server.service.UserService;
import com.kirksova.server.util.UserEntityConverter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RestApiController {

    private static final Logger log = Logger.getLogger(WebSocketEventListener.class);
    private final UserService userService;
    private final UserEntityService userEntityService;
    private final MessageService messageService;
    @Value("${message.disconnectedOfTheAgent}")
    private String disconnectedOfTheAgent;
    @Value("${message.disconnectedOfTheClient}")
    private String disconnectedOfTheClient;
    @Value("${message.correctRegistration}")
    private String correctRegistration;
    @Value("${message.correctSignInData}")
    private String correctSignInData;

    @Autowired
    public RestApiController(UserService userService, UserEntityService userEntityService,
        MessageService messageService) {
        this.userService = userService;
        this.userEntityService = userEntityService;
        this.messageService = messageService;
    }

    //Retrieve all registered agents
    @RequestMapping(value = "/agent/all", method = RequestMethod.GET)
    public ResponseEntity<List<User>> listAllUsers(
        @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        UserEntityConverter userEntityConverter = new UserEntityConverter();
        List<UserEntity> users = userEntityService.getAllUserByRole(User.TypeOfUser.AGENT);
        List<User> userList = new ArrayList<>();
        for (UserEntity user : users) {
            userList.add(userEntityConverter.convertUserEntityToUser(user));
        }
        if (pageSize != null) {
            if (pageSize > 0) {
                userList = userList.subList(pageSize * (pageNumber - 1), pageSize * pageNumber);
            }
        }
        return new ResponseEntity<>(userList, HttpStatus.OK);
    }

    //Retrieve all free agents
    @RequestMapping(value = "/agent/free", method = RequestMethod.GET)
    public ResponseEntity<List<User>> listAllFreeAgents(
        @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<User> agents = UserService.getOnlineAgents().stream().filter(User::isFreeAgent)
            .collect(Collectors.toList());
        if (pageSize != null) {
            if (pageSize > 0) {
                agents = agents.subList(pageSize * (pageNumber - 1) - 1, pageSize * pageNumber - 1);
            }
        }
        return new ResponseEntity<>(agents, HttpStatus.OK);
    }

    //Retrieve single agent
    @RequestMapping(value = "/agent/{id}", method = RequestMethod.GET)
    public ResponseEntity<User> getAgent(@PathVariable("id") long id) {
        UserEntityConverter userEntityConverter = new UserEntityConverter();
        UserEntity userEntity = userEntityService.getUserById(id);
        User user = userEntityConverter.convertUserEntityToUser(userEntity);
        if (user == null || user.getUserType() != User.TypeOfUser.AGENT) {
            return new ResponseEntity(new CustomErrorType("Agent with id " + id + " not found"), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    //Retrieve count free agents
    @RequestMapping(value = "/agent/count", method = RequestMethod.GET)
    public ResponseEntity<Integer> countFreeAgent() {
        List<User> agents = UserService.getOnlineAgents().stream().filter(User::isFreeAgent)
            .collect(Collectors.toList());
        return new ResponseEntity<>(agents.size(), HttpStatus.OK);
    }

    //Retrieve all open dialogs
    @RequestMapping(value = "/dialog", method = RequestMethod.GET)
    public ResponseEntity<List<Dialog>> allDialog(
        @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<User> agents = UserService.getOnlineAgents().stream().filter(User::isFreeAgent)
            .filter(user -> user.getInterlocutorList().size() > 0).collect(Collectors.toList());
        List<Dialog> dialogs = new ArrayList<>();
        for (User agent : agents) {
            for (Map.Entry<Long, User> entry : agent.getInterlocutorList().entrySet()) {
                Dialog dialog = new Dialog();
                dialog.setAgent(agent.getId());
                dialog.setClient(entry.getKey());
                dialogs.add(dialog);
            }
        }
        if (pageSize != null) {
            if (pageSize > 0) {
                dialogs = dialogs.subList(pageSize * (pageNumber - 1) - 1, pageSize * pageNumber - 1);
            }
        }
        return new ResponseEntity<>(dialogs, HttpStatus.OK);
    }

    //Retrieve single dialogs
    @RequestMapping(value = "/dialog/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> dialog(@PathVariable("id") long id) {
        User client = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(id)).findFirst()
            .get();
        User agent = null;
        List<User> agents = UserService.getOnlineAgents().stream().filter(User::isFreeAgent)
            .filter(user -> user.getInterlocutorList().size() > 0).collect(Collectors.toList());
        for (User agent1 : agents) {
            for (Map.Entry<Long, User> entry : agent1.getInterlocutorList().entrySet()) {
                if (entry.getKey() == id) {
                    agent = agent1;
                }
            }
        }
        if (agent == null) {
            return new ResponseEntity(new CustomErrorType("Client with id " + id + " not has dialog"),
                HttpStatus.NOT_FOUND);
        }
        List<User> dialogUser = new ArrayList<>();
        dialogUser.add(agent);
        dialogUser.add(client);
        return new ResponseEntity<>(dialogUser, HttpStatus.OK);
    }

    //Retrieve all clients in queue
    @RequestMapping(value = "/client/queue", method = RequestMethod.GET)
    public ResponseEntity<List<User>> listAllClientInQueue(
        @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<User> clients = userService.getClientsInQueue();
        if (pageSize != null) {
            if (pageSize > 0) {
                clients = clients.subList(pageSize * (pageNumber - 1) - 1, pageSize * pageNumber - 1);
            }
        }
        return new ResponseEntity<>(clients, HttpStatus.OK);
    }

    //Retrieve single client
    @RequestMapping(value = "/client/{id}", method = RequestMethod.GET)
    public ResponseEntity<User> getClient(@PathVariable("id") long id) {
        UserEntityConverter userEntityConverter = new UserEntityConverter();
        UserEntity userEntity = userEntityService.getUserById(id);
        User user = userEntityConverter.convertUserEntityToUser(userEntity);
        if (user == null || user.getUserType() != User.TypeOfUser.CLIENT) {
            return new ResponseEntity(new CustomErrorType("Client with id " + id + " not found"), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    //Register agent
    @RequestMapping(value = "/register/agent", method = RequestMethod.POST)
    public ResponseEntity<Answer> registerAgent(@RequestBody UserRest user) {
        Message message = new Message(null, "/register agent " + user.getName(), Message.MessageType.MESSAGE_CHAT);
        if (userService.registerUser(message).getTypeOfMessage() == Message.MessageType.INCORRECT_REGISTRATION_DATA) {
            return new ResponseEntity(new CustomErrorType("Unable to create. A agent with name " +
                user.getName() + " already exist."), HttpStatus.CONFLICT);
        }
        UserEntity userEntity = userEntityService.getUserByName(user.getName());
        message = new Message(userEntity.getId(), user.getPassword(), Message.MessageType.MESSAGE_CHAT);
        userService.setPassword(message);
        UserEntityConverter userEntityConverter = new UserEntityConverter();
        User user1 = userEntityConverter.convertUserEntityToUser(userEntity);
        message = new Message(userEntity.getId(), String.valueOf(user.getMaxClientCount()),
            Message.MessageType.MESSAGE_CHAT);
        if (!"0".equals(message.getText())) {
            userService.setMaxClientCount(message);
        } else {
            user1.setMaxClientCount(1);
        }
        user1.setFreeAgent(true);
        UserService.getOnlineAgents().add(user1);
        userService.startDialogue(message, user1);
        Answer answer = new Answer();
        answer.setAnswer(correctRegistration + " Your id " + user1.getId());
        return new ResponseEntity<>(answer, HttpStatus.OK);
    }

    //Login agent
    @RequestMapping(value = "/login/agent", method = RequestMethod.PUT)
    public ResponseEntity<Answer> signInAgent(@RequestBody UserRest user) {
        Message message = new Message(null, "/sign in agent " + user.getName(), Message.MessageType.MESSAGE_CHAT);
        if (userService.validateUserNameSignIn(message).getTypeOfMessage()
            == Message.MessageType.INCORRECT_LOGIN_NAME) {
            return new ResponseEntity(new CustomErrorType("Unable to sing in. A agent with name " +
                user.getName() + " is not already exist."), HttpStatus.CONFLICT);
        }
        UserEntity userEntity = userEntityService.getUserByName(user.getName());
        message = new Message(userEntity.getId(), user.getPassword(), Message.MessageType.MESSAGE_CHAT);
        if (userService.validateUserPasswordSignIn(message).getTypeOfMessage()
            == Message.MessageType.INCORRECT_LOGIN_PASSWORD) {
            return new ResponseEntity(new CustomErrorType("Unable to sing in. Incorrect password"),
                HttpStatus.CONFLICT);
        }
        message = new Message(userEntity.getId(), String.valueOf(user.getMaxClientCount()),
            Message.MessageType.MESSAGE_CHAT);
        if (!"0".equals(message.getText())) {
            userService.setMaxClientCount(message);
        }
        UserEntityConverter userEntityConverter = new UserEntityConverter();
        User user1 = userEntityConverter.convertUserEntityToUser(userEntity);
        user1.setFreeAgent(true);
        UserService.getOnlineAgents().add(user1);
        userService.startDialogue(message, user1);
        Answer answer = new Answer();
        answer.setAnswer(correctSignInData + " Your id " + user1.getId());
        return new ResponseEntity<>(answer, HttpStatus.OK);
    }

    //Register client
    @RequestMapping(value = "/register/client", method = RequestMethod.POST)
    public ResponseEntity<Answer> registerClient(@RequestBody UserRest user) {
        Message message = new Message(null, "/register client " + user.getName(), Message.MessageType.MESSAGE_CHAT);
        if (userService.registerUser(message).getTypeOfMessage() == Message.MessageType.INCORRECT_REGISTRATION_DATA) {
            return new ResponseEntity(new CustomErrorType("Unable to create. A client with name " +
                user.getName() + " already exist."), HttpStatus.CONFLICT);
        }
        UserEntity userEntity = userEntityService.getUserByName(user.getName());
        message = new Message(userEntity.getId(), user.getPassword(), Message.MessageType.MESSAGE_CHAT);
        userService.setPassword(message);
        UserEntityConverter userEntityConverter = new UserEntityConverter();
        User user1 = userEntityConverter.convertUserEntityToUser(userEntity);
        UserService.getOnlineClients().add(user1);
        Answer answer = new Answer();
        answer.setAnswer(correctRegistration + " Your id " + user1.getId());
        return new ResponseEntity<>(answer, HttpStatus.OK);
    }

    //Login client
    @RequestMapping(value = "/login/client", method = RequestMethod.PUT)
    public ResponseEntity<Answer> signInClient(@RequestBody UserRest user) {
        Message message = new Message(null, "/sign in client " + user.getName(), Message.MessageType.MESSAGE_CHAT);
        if (userService.validateUserNameSignIn(message).getTypeOfMessage()
            == Message.MessageType.INCORRECT_LOGIN_NAME) {
            return new ResponseEntity(new CustomErrorType("Unable to sing in. A client with name " +
                user.getName() + " is not already exist."), HttpStatus.CONFLICT);
        }
        UserEntity userEntity = userEntityService.getUserByName(user.getName());
        message = new Message(userEntity.getId(), user.getPassword(), Message.MessageType.MESSAGE_CHAT);
        if (userService.validateUserPasswordSignIn(message).getTypeOfMessage()
            == Message.MessageType.INCORRECT_LOGIN_PASSWORD) {
            return new ResponseEntity(new CustomErrorType("Unable to sing in. Incorrect password"),
                HttpStatus.CONFLICT);
        }
        UserEntityConverter userEntityConverter = new UserEntityConverter();
        User user1 = userEntityConverter.convertUserEntityToUser(userEntity);
        UserService.getOnlineClients().add(user1);
        Answer answer = new Answer();
        answer.setAnswer(correctSignInData + " Your id " + user1.getId());
        return new ResponseEntity<>(answer, HttpStatus.OK);
    }

    //Send message
    @RequestMapping(value = "/sendMessage", method = RequestMethod.POST)
    public ResponseEntity<?> sendMessage(@RequestBody Message message) {
        Long senderId = message.getSenderId();
        User user = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(senderId)).findFirst()
            .orElse(UserService.getOnlineAgents().stream().filter(user1 -> user1.getId().equals(senderId)).findFirst()
                .orElse(null));
        if (message.getTo() == null) {
            user.getMessagesForInterlocutor().add(message);
            message = userService.searchForAnInterlocutor(message);
            return new ResponseEntity<>(message, HttpStatus.OK);
        }
        User interlocutor = user.getInterlocutorList().get(message.getTo());
        Long toId = message.getTo();
        if (interlocutor == null) {
            if (user.getUserType() == User.TypeOfUser.CLIENT) {
                interlocutor = UserService.getOnlineAgents().stream()
                    .filter(user1 -> user1.getId().equals(toId)).findFirst().orElse(null);
            } else {
                interlocutor = UserService.getOnlineClients().stream()
                    .filter(user1 -> user1.getId().equals(toId)).findFirst().orElse(null);
                if (interlocutor != null) {
                    user.getInterlocutorList().put(toId, interlocutor);
                }
            }
        }
        if (interlocutor != null) {
            if (interlocutor.getUserSocket() != null) {
                messageService.sendMessageToSocket(interlocutor, message);
                return new ResponseEntity<String>(HttpStatus.OK);
            }
            if (interlocutor.getMessagingTemplate() != null) {
                messageService.sendMessageToWeb(interlocutor, message);
                return new ResponseEntity<String>(HttpStatus.OK);
            }
            interlocutor.getMessagesForInterlocutor().add(message);
            return new ResponseEntity<String>(HttpStatus.OK);
        } else {
            Message serverMessage = userService.searchForAnInterlocutor(message);
            user.getMessageWithoutAgent().add(message);
            user.getMessagesForInterlocutor().add(serverMessage);
            if(serverMessage.getTypeOfMessage() == Message.MessageType.CONNECTED_AGENT){
                message = userService.getAgentMessageAboutNewDialog(message);
                if (interlocutor.getUserSocket() != null) {
                    messageService.sendMessageToSocket(interlocutor, message);
                    return new ResponseEntity<String>(HttpStatus.OK);
                }
                if (interlocutor.getMessagingTemplate() != null) {
                    messageService.sendMessageToWeb(interlocutor, message);
                    return new ResponseEntity<String>(HttpStatus.OK);
                }
                interlocutor.getMessagesForInterlocutor().add(message);
            }
            return new ResponseEntity<String>(HttpStatus.OK);
        }
    }

    //Get message user
    @RequestMapping(value = "/getMessage/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getMessage(@PathVariable("id") long id) {
        List<Message> messageList = new ArrayList<>();
        User user = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(id)).findFirst()
            .orElse(UserService.getOnlineAgents().stream().filter(user1 -> user1.getId().equals(id)).findFirst()
                .orElse(null));
        if (user.getUserType() == User.TypeOfUser.CLIENT) {
            messageList.addAll(user.getMessagesForInterlocutor());
            user.getMessagesForInterlocutor().clear();
            if (messageList.size() != 0) {
                return new ResponseEntity<>(messageList, HttpStatus.OK);
            }
        }
        for (int i = 0; i < UserService.getOnlineClients().size(); i++) {
            User interlocutor = UserService.getOnlineClients().get(i);
            messageList.addAll(interlocutor.getMessagesForInterlocutor().stream()
                .filter(message1 -> Objects.equals(message1.getTo(), id))
                .collect(Collectors.toList()));
            messageList.addAll(user.getMessagesForInterlocutor());
            user.getMessagesForInterlocutor().clear();
        }
        return new ResponseEntity<>(messageList, HttpStatus.OK);
    }

    //Leave in the dialog
    @RequestMapping(value = "/leave", method = RequestMethod.POST)
    public ResponseEntity<?> leave(@RequestBody Message message) {
        User user = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
            .findFirst()
            .orElse(UserService.getOnlineAgents().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
                .findFirst().orElse(null));
        if (user.getUserType() == User.TypeOfUser.CLIENT) {
            User interlocutor = UserService.getOnlineAgents().stream()
                .filter(user1 -> user1.getId().equals(message.getTo())).findFirst().get();
            Message checkValidLeave = messageService.checkValidLeave(user, interlocutor);
            if (checkValidLeave.getTypeOfMessage() == Message.MessageType.LEAVE_CLIENT) {
                if (interlocutor.getMessagingTemplate() != null) {
                    messageService
                        .sendMessageToWeb(interlocutor, messageService.endDialogMessage(message.getTo(), user));
                    messageService.sendMessageToWeb(interlocutor, checkValidLeave);
                }
                if (interlocutor.getUserSocket() != null) {
                    messageService
                        .sendMessageToSocket(interlocutor, messageService.endDialogMessage(message.getTo(), user));
                    messageService.sendMessageToSocket(interlocutor, checkValidLeave);
                }
                if (interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null) {
                    interlocutor.getMessagesForInterlocutor()
                        .add(messageService.endDialogMessage(message.getTo(), user));
                    interlocutor.getMessagesForInterlocutor().add(checkValidLeave);
                }
                return new ResponseEntity<>(messageService.endDialogMessage(message.getSenderId(), interlocutor),
                    HttpStatus.OK);
            }
            return new ResponseEntity<>(checkValidLeave, HttpStatus.OK);
        } else {
            User interlocutor = UserService.getOnlineClients()
                .get(UserService.getOnlineClients().indexOf(message.getTo()));
            return new ResponseEntity<>(messageService.checkValidLeave(user, interlocutor), HttpStatus.OK);
        }
    }

    //Exit in the system
    @RequestMapping(value = "/exit", method = RequestMethod.POST)
    public ResponseEntity<?> exit(@RequestBody Message message) {
        User user = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
            .findFirst()
            .orElse(UserService.getOnlineAgents().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
                .findFirst().orElse(null));
        if (user == null) {
            return new ResponseEntity(new CustomErrorType("User not online"), HttpStatus.NOT_FOUND);
        }
        if (user.getUserType() == User.TypeOfUser.CLIENT) {
            log.info("Disconnect client " + user.getName());
            if (message.getTo() != null && message.getTo() != 0) {
                User interlocutor = UserService.getOnlineAgents().stream()
                    .filter(user1 -> user1.getId().equals(message.getTo())).findFirst().get();
                Message disconnectedMessage = new Message(interlocutor.getId(), disconnectedOfTheAgent,
                    Message.MessageType.DISCONNECTION_OF_THE_AGENT);
                if (interlocutor.getMessagingTemplate() != null) {
                    messageService
                        .sendMessageToWeb(interlocutor, messageService.endDialogMessage(message.getTo(), user));
                    messageService.sendMessageToWeb(interlocutor, disconnectedMessage);
                }
                if (interlocutor.getUserSocket() != null) {
                    messageService
                        .sendMessageToSocket(interlocutor, messageService.endDialogMessage(message.getTo(), user));
                    messageService.sendMessageToSocket(interlocutor, disconnectedMessage);
                }
                if (interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null) {
                    interlocutor.getMessagesForInterlocutor()
                        .add(messageService.endDialogMessage(message.getTo(), user));
                    interlocutor.getMessagesForInterlocutor().add(disconnectedMessage);
                }
            }
            UserService.getOnlineClients().remove(user);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            log.info("Disconnect agent " + user.getName());
            for (Map.Entry<Long, User> entry : user.getInterlocutorList().entrySet()) {
                User interlocutor = entry.getValue();
                log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName()
                    + " was over");
                Message disconnectedMessage = new Message(interlocutor.getId(), disconnectedOfTheAgent,
                    Message.MessageType.DISCONNECTION_OF_THE_AGENT);
                if (interlocutor.getMessagingTemplate() != null) {
                    messageService
                        .sendMessageToWeb(interlocutor, messageService.endDialogMessage(message.getTo(), user));
                    messageService.sendMessageToWeb(interlocutor, disconnectedMessage);
                }
                if (interlocutor.getUserSocket() != null) {
                    messageService
                        .sendMessageToSocket(interlocutor, messageService.endDialogMessage(message.getTo(), user));
                    messageService.sendMessageToSocket(interlocutor, disconnectedMessage);
                }
                if (interlocutor.getMessagingTemplate() == null && interlocutor.getUserSocket() == null) {
                    interlocutor.getMessagesForInterlocutor()
                        .add(messageService.endDialogMessage(message.getTo(), user));
                    interlocutor.getMessagesForInterlocutor().add(disconnectedMessage);
                }
            }
            user.getInterlocutorList().clear();
            UserService.getOnlineAgents().remove(user);
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }
}
