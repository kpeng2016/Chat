package com.kirksova.server.service;

import com.kirksova.server.model.UserEntity;
import com.kirksova.server.queue.ClientQueue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:configuration.properties")
public class UserServiceTest {

    @Value("${host.topic}")
    private String topic;
    @Value("${user}")
    private String sessionUser;
    @Value("${interlocutor}")
    private String sessionInterlocutor;
    @Value("${message.noFreeAgent}")
    private String noFreeAgent;
    @Value("${message.wait}")
    private String wait;
    @Value("${message.firstMessageAgent}")
    private String firstMessageAgent;
    @Value("${message.connectedAgent}")
    private String connectedAgent;
    @Value("${message.connectedClient}")
    private String connectedClient;
    @Value("${message.noClientInQueue}")
    private String noClientInQueue;
    @Value("${message.correctRegistration}")
    private String correctRegistration;
    @Value("${message.incorrectRegistrationData}")
    private String incorrectRegistrationData;
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

    @Mock
    private UserEntity userEntity;
    @Mock
    private MessageService messageService;
    @Mock
    private UserEntityService userEntityService;
    @Mock
    private SimpMessageSendingOperations messagingTemplate;
    @Mock
    private SimpMessageHeaderAccessor headerAccessor;
    @Mock
    private ClientQueue clientQueue;

    private UserService unitUnderTests;

    @Before
    public void setUp() {
        unitUnderTests = new UserService();
    }

    /*@Test
    public void registerUser() {
        Message message = new Message(null, "/register agent me", Message.MessageType.MESSAGE_CHAT);
        Message returnRegisterUser = new Message(1L, correctRegistration, Message.MessageType.CORRECT_REGISTRATION);
        when(userEntityService.existsUserWithName(message.getText())).thenReturn(false);
        when(userEntityService.create(userEntity)).thenReturn(userEntity);
        Message result = unitUnderTests.registerUser(message);
        assertEquals(returnRegisterUser, result);

        message = new Message(null, "/register client me", Message.MessageType.MESSAGE_CHAT);
        returnRegisterUser = new Message(1L, correctRegistration, Message.MessageType.CORRECT_REGISTRATION);
        when(userEntityService.existsUserWithName(message.getText())).thenReturn(true);
        when(userEntityService.create(userEntity)).thenReturn(userEntity);
        result = unitUnderTests.registerUser(message);
        assertEquals(returnRegisterUser, result);

        message = new Message(null, "register agent me", Message.MessageType.MESSAGE_CHAT);
        returnRegisterUser = new Message(null, incorrectRegistrationData, Message.MessageType.INCORRECT_REGISTRATION_DATA);
        result = unitUnderTests.registerUser(message);
        assertEquals(returnRegisterUser, result);
    }*/

    @Test
    public void setPassword() {
    }

    @Test
    public void validateUserNameSignIn() {
    }

    @Test
    public void validateUserPasswordSignIn() {
    }

    @Test
    public void startDialogue() {
    }

    @Test
    public void setMaxClientCount() {
    }

    @Test
    public void deleteClientInQueue() {
    }

    @Test
    public void searchForAnInterlocutor() {
    }

    @Test
    public void getAgentMessageAboutNewDialog() {
    }

    @Test
    public void getClientMessageAboutNewDialog() {
    }

    @Test
    public void setClientSession() {
    }

    @Test
    public void setAgentSession() {
    }

    @Test
    public void getOnlineAgents() {
    }

    @Test
    public void getOnlineClients() {
    }

}