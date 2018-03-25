package com.kirksova.server.util;

import com.kirksova.server.model.User;
import com.kirksova.server.model.UserEntity;

/**
 * Класс для преобразования User в UserEntity и наоборот
 */
public class UserEntityConverter {

    public User convertUserEntityToUser(UserEntity userEntity){
        User user = new User();
        user.setUserType(userEntity.getUserType());
        user.setId(userEntity.getId());
        user.setName(userEntity.getName());
        user.setPassword(userEntity.getPassword());
        user.setMaxClientCount(userEntity.getMaxClientCount());
        return user;
    }

    public UserEntity convertUserToUserEntity(User user){
        UserEntity userEntity = new UserEntity();
        userEntity.setId(user.getId());
        userEntity.setName(user.getName());
        userEntity.setPassword(user.getPassword());
        userEntity.setUserType(user.getUserType());
        userEntity.setMaxClientCount(user.getMaxClientCount());
        return userEntity;
    }

}
