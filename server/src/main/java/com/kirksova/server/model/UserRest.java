package com.kirksova.server.model;

public class UserRest {

    private String password;
    private String name;
    private int maxClientCount;

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public int getMaxClientCount() {
        return maxClientCount;
    }
}
