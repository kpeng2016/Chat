package com.kirksova.server.model;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository
public class UserEntityDao {

    private static final String SELECT_BY_USERNAME_QUERY = "from UserEntity u where u.name = :username";
    private static final String SELECT_BY_ROLE_QUERY = "from UserEntity u where u.userType = :role";
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

    public List<UserEntity> getAllUserByRole(User.TypeOfUser role) {
        Query query = entityManager.createQuery(SELECT_BY_ROLE_QUERY, UserEntity.class);
        query.setParameter("role", role);
        return query.getResultList();
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
