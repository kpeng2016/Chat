package com.kirksova.server.model;

import javax.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity {

    private String name;
    private String password;
    private User.TypeOfUser userType;
    private int maxClientCount = 0;
    private String salt;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User.TypeOfUser getUserType() {
        return userType;
    }

    public void setUserType(User.TypeOfUser userType) {
        this.userType = userType;
    }

    public int getMaxClientCount() {
        return maxClientCount;
    }

    public void setMaxClientCount(int maxClientCount) {
        this.maxClientCount = maxClientCount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
