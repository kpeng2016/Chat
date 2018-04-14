package com.kirksova.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс для информации о пользователях.
 */

public class User {

    @JsonIgnore
    private List<Message> messageWithoutAgent = new ArrayList<>();
    @JsonIgnore
    private List<Message> messagesForInterlocutor = new ArrayList<>();
    @JsonIgnore
    private Map<Long, User> interlocutorList = new HashMap<>();
    private long id;
    @JsonIgnore
    private String password;
    private String name;
    private TypeOfUser userType;
    private int maxClientCount;
    @JsonIgnore
    private Socket userSocket;
    @JsonIgnore
    private SimpMessageSendingOperations messagingTemplate;
    private boolean freeAgent;
    @JsonIgnore
    private int clientCountNow = 0;
    @JsonIgnore
    private int clientCountTotal = 0;

    public void setFreeAgent(boolean freeAgent) {
        this.freeAgent = userType == TypeOfUser.AGENT && freeAgent;
    }

    public boolean isFreeAgent() {
        return freeAgent;
    }

    public TypeOfUser getUserType() {
        return userType;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public Integer getClientCountTotal() {
        return clientCountTotal;
    }

    public void iterateClientCountTotal() {
        this.clientCountTotal++;
    }

    public void iterateClientCountNow() {
        this.clientCountNow++;
    }

    public void deleteClientCountNow() {
        this.clientCountNow--;
    }

    public Socket getUserSocket() {
        return userSocket;
    }

    public void setUserSocket(Socket userSocket) {
        this.userSocket = userSocket;
    }

    public SimpMessageSendingOperations getMessagingTemplate() {
        return messagingTemplate;
    }

    public void setMessagingTemplate(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public int getMaxClientCount() {
        return maxClientCount;
    }

    public void setMaxClientCount(int maxClientCount) {
        this.maxClientCount = maxClientCount;
    }

    public int getClientCountNow() {
        return clientCountNow;
    }

    public String getPassword() {
        return password;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserType(TypeOfUser userType) {
        this.userType = userType;
    }

    public Map<Long, User> getInterlocutorList() {
        return interlocutorList;
    }

    public List<Message> getMessagesForInterlocutor() {
        return messagesForInterlocutor;
    }

    public List<Message> getMessageWithoutAgent() {
        return messageWithoutAgent;
    }

    public enum TypeOfUser {
        AGENT, CLIENT
    }
}
