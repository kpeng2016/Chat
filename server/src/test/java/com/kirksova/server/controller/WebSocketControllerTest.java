package com.kirksova.server.controller;

/*import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class WebSocketControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private MessageService messageService;
    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private SimpMessageHeaderAccessor headerAccessor;

    private WebSocketController unitUnderTests;

    @Before
    public void setUp() throws Exception {
        unitUnderTests = new WebSocketController(messagingTemplate, messageService, userService);
    }

    @Test
    public void sendMessageInterlocutor() {
        // Here mock logic
        //prepare test values
        unitUnderTests.sendMessageInterlocutor(new Message(Long.valueOf(1), "/register agent me", MessageType.REGISTER),
            headerAccessor);
        //verify().register();
        //tests results validation
        //verify().;

    }

    @Test
    public void addUser() {
        Message message = new Message(null, "/register agent me", MessageType.REGISTER);
        User user = new User(User.TypeOfUser.AGENT, "me", null);
        when(userService.registerUser(message)).thenReturn(user);
        Message result = unitUnderTests.addUser(message, headerAccessor);
        assertEquals(1L, (long) result.getSenderId());
        assertEquals(MessageType.CORRECT_REGISTRATION, result.getTypeOfMessage());

        message = new Message(null, "register agent me", MessageType.REGISTER);
        when(userService.registerUser(message)).thenReturn(null);
        result = unitUnderTests.addUser(message, headerAccessor);
        assertEquals(null, result.getSenderId());
        assertEquals(MessageType.INCORRECT_REGISTRATION_DATA, result.getTypeOfMessage());
    }

    @Test
    public void searchForAnInterlocutor() {
        Message message = new Message(1L, "hi", MessageType.MESSAGE_CHAT);
        Message messageForVerify = new Message(1L,
            "Please wait, there are no free agents now. All you sent messages will be forwarded to agent on connect.",
            MessageType.NO_FREE_AGENT);
        when(userService.searchForAnInterlocutor(message)).thenReturn(messageForVerify);
        unitUnderTests.searchForAnInterlocutor(message, headerAccessor);
        verify(messagingTemplate, times(1)).convertAndSend("null" + message.getSenderId(), messageForVerify);
    }

    @Test
    public void leave() {
    }
}*/