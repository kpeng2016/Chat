package com.kirksova.server.service;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.User;
import com.kirksova.server.model.enumType.TypeOfMessage;
import com.kirksova.server.model.enumType.TypeOfUser;
import org.apache.log4j.Logger;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

@Service
public class RegistrationUser {

    private static final Logger log = Logger.getLogger(RegistrationUser.class);
    private Message messageRegistration;
    private User user;
    private SimpMessageHeaderAccessor headerAccessor;

    public boolean register() {
        String text = messageRegistration.getText();
        if (messageRegistration.getTypeOfMessage() == TypeOfMessage.Register) {
            if (text.matches("/register agent .+")) {
                //добавить пользователя в список агентов, по умолчанию сделать свободным
                String[] name = text.trim().split("/register agent ");
                user = new User(TypeOfUser.agent, name[1]);
                log.info("Register agent " + name[1]);
                user.setFreeAgent(true);
                user.getAgents().add(user);
                headerAccessor.getSessionAttributes().put("User", user);
                return true;
            }
            if (text.matches("/register client .+")) {
                //добавить ползователя в список клиентов
                String[] name = text.trim().split("/register client ");
                user = new User(TypeOfUser.client, name[1]);
                log.info("Register client " + name[1]);
                user.getClients().add(user);
                headerAccessor.getSessionAttributes().put("User", user);
                return true;
            }
        }
        return false;
    }

    public User getUser() {
        return user;
    }

    public void setMessageRegistration(Message messageRegistration) {
        this.messageRegistration = messageRegistration;
    }

    public void setHeaderAccessor(SimpMessageHeaderAccessor headerAccessor) {
        this.headerAccessor = headerAccessor;
    }
}
