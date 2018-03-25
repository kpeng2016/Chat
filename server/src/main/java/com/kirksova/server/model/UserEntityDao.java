package com.kirksova.server.model;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Repository
public class UserEntityDao {

    public static final String SELECT_BY_USERNAME_QUERY = "from UserEntity u where u.name = :username";
    @PersistenceContext
    private EntityManager entityManager;

    public UserEntity create(UserEntity user) {
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    public void update(UserEntity user) {
        entityManager.merge(user);
    }

    public UserEntity getUserById(long id) {
        return entityManager.find(UserEntity.class, id);
    }

    public UserEntity getUserByName(String username) {
        Query query = entityManager.createQuery(SELECT_BY_USERNAME_QUERY, UserEntity.class);
        query.setParameter("username", username);
        return (UserEntity) query.getSingleResult();
    }

    public boolean existsUserWithName(String username) {
        Query query = entityManager.createQuery(SELECT_BY_USERNAME_QUERY, UserEntity.class);
        query.setParameter("username", username);
        return query.getResultList().size() > 0;
    }

    public void delete(long id) {
        UserEntity userEntity = getUserById(id);
        if (userEntity != null) {
            entityManager.remove(userEntity);
        }
    }
}
