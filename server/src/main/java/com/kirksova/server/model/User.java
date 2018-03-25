package com.kirksova.server.model;

import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.net.Socket;

/**
 * Класс для информации о пользователях.
 */

public class User {

    private long id;
    private String password;
    private String name;
    private TypeOfUser userType;
    private int maxClientCount;
    private Socket userSocket;
    private SimpMessageSendingOperations messagingTemplate;
    private boolean freeAgent;
    private int clientCountNow = 0;
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
        this.clientCountTotal++;
    }

    public void deleteClientCountNow() {
        this.clientCountTotal--;
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

    public void setClientCountNow(int clientCountNow) {
        this.clientCountNow = clientCountNow;
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

    public void setClientCountTotal(int clientCountTotal) {
        this.clientCountTotal = clientCountTotal;
    }

    public enum TypeOfUser {
        AGENT, CLIENT
    }
}
