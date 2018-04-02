package com.kirksova.server.controller;

import com.kirksova.server.ServerApplicationTests;
import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import com.kirksova.server.model.UserEntity;
import com.kirksova.server.service.MessageService;
import com.kirksova.server.service.UserEntityService;
import com.kirksova.server.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

//import org.springframework.boot.test.context.SpringBootContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:configuration.properties")
@SpringBootTest(classes = ServerApplicationTests.class, properties = "classpath:configuration.properties")
//@ContextConfiguration(classes = WebSocketController.class, loader = SpringApplicationContextLoader.class)
//@ContextConfiguration(classes=WebSocketConfig.class)
//@SpringBootTest()
//@ContextConfiguration(classes = Config, loader = SpringBootContextLoader)
//@SpringBootTest(properties = "classpath:configuration.properties")
//@SpringBootContextLoader()
public class WebSocketControllerTest {

    @Value("${host.topic}")
    private String topic;
    @Value("${message.incorrectLoginName}")
    private String incorrectLoginName;
    @Value("${message.correctSignInData}")
    private String correctSignInData;
    @Value("${message.incorrectLoginPassword}")
    private String incorrectLoginPassword;
    @Value("${message.incorrectDataMaxCountClients}")
    private String incorrectDataMaxCountClients;
    @Value("${message.correctDataMaxCountClients}")
    private String correctDataMaxCountClients;
    @Value("${message.notUpdateDataMaxCountClients}")
    private String notUpdateDataMaxCountClients;
    @Value("${message.incorrectRegistrationData}")
    private String incorrectRegistrationData;
    @Value("${message.correctRegistration}")
    private String correctRegistration;
    @Mock
    private UserService userService;
    @Mock
    private MessageService messageService;
    @Mock
    private UserEntityService userEntityService;
    @Mock
    private SimpMessageSendingOperations messagingTemplate;
    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    private WebSocketController unitUnderTests;

    @Before
    public void setUp() {
        unitUnderTests = new WebSocketController(messagingTemplate, messageService, userService, userEntityService);
    }

    @Test
    public void addUser() {
        Message message = new Message(null, "/register agent me", MessageType.MESSAGE_CHAT);
        Message returnRegisterUser = new Message(1L, correctRegistration, MessageType.CORRECT_REGISTRATION);
        when(userService.registerUser(message)).thenReturn(returnRegisterUser);
        Message result = unitUnderTests.addUser(message);
        assertEquals(returnRegisterUser, result);

        message = new Message(null, "register agent me", MessageType.MESSAGE_CHAT);
        returnRegisterUser = new Message(null, incorrectRegistrationData, MessageType.INCORRECT_REGISTRATION_DATA);
        when(userService.registerUser(message)).thenReturn(returnRegisterUser);
        result = unitUnderTests.addUser(message);
        assertEquals(returnRegisterUser, result);
    }

    @Test
    public void loginUser() {
        Message message = new Message(null, "/sign in agent me", MessageType.MESSAGE_CHAT);
        Message returnLoginUser = new Message(1L, correctSignInData, MessageType.CORRECT_LOGIN_NAME);
        when(userService.validateUserNameSignIn(message)).thenReturn(returnLoginUser);
        Message result = unitUnderTests.loginUser(message);
        assertEquals(returnLoginUser, result);

        message = new Message(null, "sign in me", MessageType.MESSAGE_CHAT);
        returnLoginUser = new Message(null, incorrectLoginName, MessageType.INCORRECT_LOGIN_NAME);
        when(userService.validateUserNameSignIn(message)).thenReturn(returnLoginUser);
        result = unitUnderTests.loginUser(message);
        assertEquals(returnLoginUser, result);
    }

    @Test
    public void setPasswordClient() {
        Message message = new Message(1L, "password", MessageType.MESSAGE_CHAT);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setUserType(User.TypeOfUser.CLIENT);
        when(userEntityService.getUserById(message.getSenderId())).thenReturn(userEntity);
        unitUnderTests.setPassword(message, headerAccessor);
        verify(userService, times(1)).setPassword(message);
        verify(userEntityService, times(1)).getUserById(message.getSenderId());
        verify(userService, times(1)).setMessagingTemplate(messagingTemplate);
        verify(userService, times(1)).setAgentSession(headerAccessor, userEntity);
        verify(userService, times(1)).setClientSession(headerAccessor, userEntity);
        verify(userService, times(0)).startDialogue(message, null);
    }

    @Test
    public void setPasswordAgent() {
        Message message = new Message(1L, "password", MessageType.MESSAGE_CHAT);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setUserType(User.TypeOfUser.AGENT);
        when(userEntityService.getUserById(message.getSenderId())).thenReturn(userEntity);
        unitUnderTests.setPassword(message, headerAccessor);
        verify(userService, times(1)).setPassword(message);
        verify(userEntityService, times(1)).getUserById(message.getSenderId());
        verify(userService, times(1)).setMessagingTemplate(messagingTemplate);
        verify(userService, times(1)).setAgentSession(headerAccessor, userEntity);
        verify(userService, times(1)).setClientSession(headerAccessor, userEntity);
        verify(userService, times(1)).startDialogue(message, null);
    }

    @Test(expected = NullPointerException.class)
    public void setPasswordException() {
        Message message = new Message(null, "password", MessageType.MESSAGE_CHAT);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setUserType(User.TypeOfUser.AGENT);
        when(userEntityService.getUserById(message.getSenderId())).thenThrow(NullPointerException.class);
        unitUnderTests.setPassword(message, headerAccessor);
        verify(userEntityService, times(1)).getUserById(message.getSenderId());
        verify(userService, times(1)).setPassword(message);
        verify(userService, times(0)).setAgentSession(headerAccessor, userEntity);
        verify(userService, times(0)).setClientSession(headerAccessor, userEntity);
        verify(userService, times(0)).startDialogue(message, null);
    }

    /*@Test
    public void checkPasswordPositive() {
        Message message = new Message(1L, "password", MessageType.MESSAGE_CHAT);
        Message returnCheckPassword = new Message(1L, correctSignInData, MessageType.CORRECT_LOGIN_PASSWORD);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setUserType(User.TypeOfUser.AGENT);
        when(userService.validateUserPasswordSignIn(message)).thenReturn(returnCheckPassword);
        when(userEntityService.getUserById(message.getSenderId())).thenReturn(userEntity);
        unitUnderTests.checkPassword(message, headerAccessor);
        verify(messagingTemplate, times(1)).convertAndSend(topic + message.getSenderId(), message);
        verify(userEntityService, times(1)).getUserById(message.getSenderId());
        verify(userService, times(1)).setMessagingTemplate(messagingTemplate);
        verify(userService, times(1)).setAgentSession(headerAccessor, userEntity);
        verify(userService, times(1)).setClientSession(headerAccessor, userEntity);
        verify(userService, times(1)).startDialogue(message, null);
    }*/

    @Test
    public void checkPasswordNegative(){

    }

    @Test
    public void setMaxClient()  {
    }


    @Test
    public void sendMessageInterlocutor() {
        // Here mock logic
        //prepare test values
        unitUnderTests
            .sendMessageInterlocutor(new Message(Long.valueOf(1), "/register agent me", MessageType.MESSAGE_CHAT),
                headerAccessor);
        //verify().register();
        //tests results validation
        //verify().;

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
}