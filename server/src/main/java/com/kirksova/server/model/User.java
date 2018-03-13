package com.kirksova.server.model;

import com.kirksova.server.model.enumType.TypeOfUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Класс для информации о пользователях.
 */

public class User {

    private static List<User> agents = Collections.synchronizedList(new ArrayList<User>());
    private static List<User> clients = Collections.synchronizedList(new ArrayList<User>());
    private static AtomicLong isGenId = new AtomicLong();

    private TypeOfUser userType;
    private String name;
    private boolean isFreeAgent;
    private int countUsers = 0;
    private long id;

    public User(TypeOfUser userT, String name) {
        this.userType = userT;
        this.name = name;
        id = isGenId.incrementAndGet();
    }

    public void setFreeAgent(boolean freeAgent) {
        isFreeAgent = userType == TypeOfUser.agent && freeAgent;
    }

    public boolean getIsFreeAgent() {
        return isFreeAgent;
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

    public Integer getCountUsers() {
        return countUsers;
    }

    public void setCountUsers(int countUsers) {
        this.countUsers = countUsers;
    }

    public List<User> getAgents() {
        return agents;
    }

    public List<User> getClients() {
        return clients;
    }
}
