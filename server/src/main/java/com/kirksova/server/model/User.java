package com.kirksova.server.model;

import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Класс для информации о пользователях.
 */

public class User {

    private static AtomicLong genId = new AtomicLong();

    private Socket userSocket;
    private SimpMessageSendingOperations messagingTemplate;
    private TypeOfUser userType;
    private String name;
    private boolean freeAgent;
    private int userCount = 0;
    private long id;

    public User(TypeOfUser userT, String name) {
        this.userType = userT;
        this.name = name;
        id = genId.incrementAndGet();
    }

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

    public Integer getUserCount() {
        return userCount;
    }

    public void iterateUserCount() {
        this.userCount++;
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

    public enum TypeOfUser {
        AGENT, CLIENT
    }
}
