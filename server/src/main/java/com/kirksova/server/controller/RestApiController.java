package com.kirksova.server.controller;

import com.kirksova.server.error.CustomErrorType;
import com.kirksova.server.model.Dialog;
import com.kirksova.server.model.Message;
import com.kirksova.server.model.User;
import com.kirksova.server.model.UserEntity;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserEntityService;
import com.kirksova.server.service.UserService;
import com.kirksova.server.util.UserEntityConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RestApiController {

    private final UserService userService;
    private final UserEntityService userEntityService;
    private final MessageService messageService;
    @Value("${message.agentCantLeave}")
    private String agentCantLeave;

    @Autowired
    public RestApiController(UserService userService, UserEntityService userEntityService,
        MessageService messageService) {
        this.userService = userService;
        this.userEntityService = userEntityService;
        this.messageService = messageService;
    }

    //Retrieve all registered agents
    @RequestMapping(value = "/agent/all", method = RequestMethod.GET, params = {"pageNumber", "pageSize"})
    public ResponseEntity<List<User>> listAllUsers(@RequestParam("pageNumber") int pageNumber,
        @RequestParam("pageSize") int pageSize) {
        UserEntityConverter userEntityConverter = new UserEntityConverter();
        List<UserEntity> users = userEntityService.getAllUserByRole(User.TypeOfUser.AGENT);
        List<User> userList = new ArrayList<>();
        for (UserEntity user : users) {
            userList.add(userEntityConverter.convertUserEntityToUser(user));
        }
        if (users.isEmpty()) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
        if (pageSize > 0) {
            userList = userList.subList(pageSize * (pageNumber - 1), pageSize * pageNumber);
        }
        return new ResponseEntity<>(userList, HttpStatus.OK);
    }

    //Retrieve all free agents
    @RequestMapping(value = "/agent/free", method = RequestMethod.GET, params = {"pageNumber", "pageSize"})
    public ResponseEntity<List<User>> listAllFreeAgents(@RequestParam("pageNumber") int pageNumber,
        @RequestParam("pageSize") int pageSize) {
        List<User> agents = UserService.getOnlineAgents().stream().filter(User::isFreeAgent)
            .collect(Collectors.toList());
        if (agents.isEmpty()) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
        if (pageSize > 0) {
            agents = agents.subList(pageSize * (pageNumber - 1) - 1, pageSize * pageNumber - 1);
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
    @RequestMapping(value = "/dialog", method = RequestMethod.GET, params = {"pageNumber", "pageSize"})
    public ResponseEntity<List<Dialog>> allDialog(@RequestParam("pageNumber") int pageNumber,
        @RequestParam("pageSize") int pageSize) {
        List<User> agents = UserService.getOnlineAgents().stream().filter(User::isFreeAgent)
            .filter(user -> user.getClientsAgent().size() > 0).collect(Collectors.toList());
        List<Dialog> dialogs = new ArrayList<>();
        for (User agent : agents) {
            for (Map.Entry<Long, User> entry : agent.getClientsAgent().entrySet()) {
                Dialog dialog = new Dialog();
                dialog.setAgent(agent.getId());
                dialog.setClient(entry.getKey());
                dialogs.add(dialog);
            }
        }
        if (pageSize > 0){
            dialogs = dialogs.subList(pageSize*(pageNumber-1) - 1, pageSize*pageNumber - 1);
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
            .filter(user -> user.getClientsAgent().size() > 0).collect(Collectors.toList());
        for (User agent1 : agents) {
            for (Map.Entry<Long, User> entry : agent1.getClientsAgent().entrySet()) {
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
    @RequestMapping(value = "/client/queue", method = RequestMethod.GET, params = {"pageNumber", "pageSize"})
    public ResponseEntity<List<User>> listAllClientInQueue(@RequestParam("pageNumber") int pageNumber,
        @RequestParam("pageSize") int pageSize) {
        List<User> clients = userService.getClientsInQueue();
        if (pageSize > 0){
            clients = clients.subList(pageSize*(pageNumber-1) - 1, pageSize*pageNumber - 1);
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
    public ResponseEntity<?> registerAgent(@RequestBody User user) {
        Message message = new Message(null, "/register agent" + user.getName(), Message.MessageType.MESSAGE_CHAT);
        if (userService.registerUser(message).getTypeOfMessage() == Message.MessageType.INCORRECT_REGISTRATION_DATA) {
            return new ResponseEntity(new CustomErrorType("Unable to create. A User with name " +
                user.getName() + " already exist."), HttpStatus.CONFLICT);
        }
        UserEntity userEntity = userEntityService.getUserByName(user.getName());
        message = new Message(userEntity.getId(), user.getPassword(), Message.MessageType.MESSAGE_CHAT);
        userService.setPassword(message);
        message = new Message(userEntity.getId(), String.valueOf(user.getMaxClientCount()),
            Message.MessageType.MESSAGE_CHAT);
        userService.setMaxClientCount(message);
        userService.startDialogue(message, user);
        return new ResponseEntity<String>(HttpStatus.CREATED);
    }

    //Login agent
    @RequestMapping(value = "/login/agent", method = RequestMethod.PUT)
    public ResponseEntity<?> signInAgent(@RequestBody User user) {
        Message message = new Message(null, "/sign in agent" + user.getName(), Message.MessageType.MESSAGE_CHAT);
        if (userService.validateUserNameSignIn(message).getTypeOfMessage()
            == Message.MessageType.INCORRECT_LOGIN_NAME) {
            return new ResponseEntity(new CustomErrorType("Unable to sing in. A User with name " +
                user.getName() + " is not already exist."), HttpStatus.CONFLICT);
        }
        UserEntity userEntity = userEntityService.getUserByName(user.getName());
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedString = encoder.encodeToString(user.getPassword().getBytes());
        message = new Message(userEntity.getId(), encodedString, Message.MessageType.MESSAGE_CHAT);
        userService.validateUserPasswordSignIn(message);
        message = new Message(userEntity.getId(), String.valueOf(user.getMaxClientCount()),
            Message.MessageType.MESSAGE_CHAT);
        userService.setMaxClientCount(message);
        userService.startDialogue(message, user);
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    //Register client
    @RequestMapping(value = "/register/client", method = RequestMethod.POST)
    public ResponseEntity<?> registerClient(@PathVariable User user) {
        Message message = new Message(null, "/register client" + user.getName(), Message.MessageType.MESSAGE_CHAT);
        if (userService.registerUser(message).getTypeOfMessage() == Message.MessageType.INCORRECT_REGISTRATION_DATA) {
            return new ResponseEntity(new CustomErrorType("Unable to create. A User with name " +
                user.getName() + " already exist."), HttpStatus.CONFLICT);
        }
        UserEntity userEntity = userEntityService.getUserByName(user.getName());
        message = new Message(userEntity.getId(), user.getPassword(), Message.MessageType.MESSAGE_CHAT);
        userService.setPassword(message);
        message = new Message(userEntity.getId(), String.valueOf(user.getMaxClientCount()),
            Message.MessageType.MESSAGE_CHAT);
        userService.setMaxClientCount(message);
        return new ResponseEntity<String>(HttpStatus.CREATED);
    }

    //Login client
    @RequestMapping(value = "/login/client", method = RequestMethod.PUT)
    public ResponseEntity<?> signInClient(@PathVariable User user) {
        Message message = new Message(null, "/sign in client" + user.getName(), Message.MessageType.MESSAGE_CHAT);
        if (userService.registerUser(message).getTypeOfMessage() == Message.MessageType.INCORRECT_LOGIN_NAME) {
            return new ResponseEntity(new CustomErrorType("Unable to create. A User with name " +
                user.getName() + " already exist."), HttpStatus.CONFLICT);
        }
        UserEntity userEntity = userEntityService.getUserByName(user.getName());
        message = new Message(userEntity.getId(), user.getPassword(), Message.MessageType.MESSAGE_CHAT);
        userService.setPassword(message);
        message = new Message(userEntity.getId(), String.valueOf(user.getMaxClientCount()),
            Message.MessageType.MESSAGE_CHAT);
        userService.setMaxClientCount(message);
        return new ResponseEntity<String>(HttpStatus.CREATED);
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
        User interlocutor = user.getClientsAgent().get(message.getTo());
        Long toId = message.getTo();
        if (interlocutor == null) {
            if (user.getUserType() == User.TypeOfUser.CLIENT) {
                interlocutor = UserService.getOnlineAgents().stream()
                    .filter(user1 -> user1.getId().equals(toId)).findFirst().get();
            } else {
                interlocutor = UserService.getOnlineClients().stream()
                    .filter(user1 -> user1.getId().equals(toId)).findFirst().get();
                user.getClientsAgent().put(toId, interlocutor);
            }
        }
        if (interlocutor.getUserSocket() != null) {
            messageService.sendMessageToSocket(interlocutor, message);
            return new ResponseEntity<String>(HttpStatus.OK);
        }
        if (interlocutor.getMessagingTemplate() != null) {
            messageService.sendMessageToWeb(interlocutor, message);
            return new ResponseEntity<String>(HttpStatus.OK);
        }
        user.getMessagesForInterlocutor().add(message);
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    //Send message
    @RequestMapping(value = "/getMessage", method = RequestMethod.GET)
    public ResponseEntity<?> getMessage(@RequestBody Message message) {
        List<Message> messageList = new ArrayList<>();
        Long senderId = message.getSenderId();
        User user = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(senderId)).findFirst()
            .orElse(UserService.getOnlineAgents().stream().filter(user1 -> user1.getId().equals(senderId)).findFirst()
                .orElse(null));
        if (user.getUserType() == User.TypeOfUser.CLIENT) {
            for (int i = 0; i < UserService.getOnlineAgents().size(); i++) {
                User interlocutor = UserService.getOnlineAgents().get(i);
                messageList = interlocutor.getMessagesForInterlocutor().stream()
                    .filter(message1 -> Objects.equals(message1.getTo(), message.getSenderId()))
                    .collect(Collectors.toList());
                if (messageList.size() != 0) {
                    return new ResponseEntity<>(messageList, HttpStatus.OK);
                }
            }
        }
        for (int i = 0; i < UserService.getOnlineClients().size(); i++) {
            User interlocutor = UserService.getOnlineClients().get(i);
            messageList.addAll(interlocutor.getMessagesForInterlocutor().stream()
                .filter(message1 -> Objects.equals(message1.getTo(), message.getSenderId()))
                .collect(Collectors.toList()));
        }
        return new ResponseEntity<>(messageList, HttpStatus.OK);
    }

    //Leave in the dialog
    @RequestMapping(value = "/leave", method = RequestMethod.GET)
    public ResponseEntity<?> leave(@RequestBody Message message) {
        User user = UserService.getOnlineClients().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
            .findFirst()
            .orElse(UserService.getOnlineAgents().stream().filter(user1 -> user1.getId().equals(message.getSenderId()))
                .findFirst()
                .orElse(null));
        if (user.getUserType() == User.TypeOfUser.CLIENT) {
            User interlocutor = UserService.getOnlineAgents().stream()
                .filter(user1 -> user1.getId().equals(message.getTo())).findFirst().get();
            return new ResponseEntity<>(messageService.checkValidLeave(user, interlocutor), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                new Message(user.getId(), agentCantLeave, Message.MessageType.AGENT_CANT_LEAVE, message.getTo(),
                    message.getNameTo()), HttpStatus.OK);
        }
    }
}
