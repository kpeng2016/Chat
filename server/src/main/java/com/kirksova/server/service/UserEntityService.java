package com.kirksova.server.service;

import com.kirksova.server.model.UserEntity;
import com.kirksova.server.model.UserEntityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserEntityService {

    @Autowired
    private UserEntityDao userEntityDao;

    public UserEntity create(UserEntity user) {
        return userEntityDao.create(user);
    }

    public void update(UserEntity user) {
        userEntityDao.update(user);
    }

    public UserEntity getUserById(long id) {
        return userEntityDao.getUserById(id);
    }

    public UserEntity getUserByName(String username) {
        return userEntityDao.getUserByName(username);
    }

    public boolean existsUserWithName(String username) {
        return userEntityDao.existsUserWithName(username);
    }

    public void delete(long id) {
        userEntityDao.delete(id);
    }
}
