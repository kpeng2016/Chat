package com.kirksova.server.service;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.User;
import com.kirksova.server.model.enumType.TypeOfMessage;
import com.kirksova.server.model.enumType.TypeOfUser;
import org.apache.log4j.Logger;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchForAnInterlocutor {

    private static final Logger log = Logger.getLogger(SearchForAnInterlocutor.class);
    private Message firstMessage;
    private SimpMessageHeaderAccessor headerAccessor;
    private SimpMessageSendingOperations messagingTemplate;

    public void searchForAnInterlocutor() {
        User user = (User) headerAccessor.getSessionAttributes().get("User");
        Long userId = user.getId();
        List<User> freeagents;
        User interlocutor;
        if (user.getUserType() == TypeOfUser.client) {
            synchronized (user.getAgents()) {
                freeagents = user.getAgents().stream().filter(User::getIsFreeAgent).collect(Collectors.toList());
                if (freeagents.size() > 0) {
                    interlocutor = freeagents.get(0);
                    for (int i = 0; i < freeagents.size() - 1; i++) {
                        if (freeagents.get(i).getCountUsers() > freeagents.get(i + 1).getCountUsers()) {
                            interlocutor = user.getAgents().get(i + 1);
                        }
                    }
                    interlocutor.setFreeAgent(false);
                    interlocutor.setCountUsers(interlocutor.getCountUsers() + 1);
                    log.info("Dialogue between agent " + interlocutor.getName() + " and client " + user.getName() + " was started");
                    headerAccessor.getSessionAttributes().put("Interlocutor", interlocutor);
                    messagingTemplate.convertAndSend("/topic/" + userId, new Message(userId, "С вами работает агент " +
                            interlocutor.getName(), TypeOfMessage.ConnectedAgent, interlocutor.getId(), interlocutor.getName()));
                    messagingTemplate.convertAndSend("/topic/" + interlocutor.getId(), new Message(interlocutor.getId(), "Подключен клиент " + user.getName(),
                            TypeOfMessage.ConnectedClient, userId, user.getName()));
                } else {
                    if (firstMessage.getTypeOfMessage() == TypeOfMessage.CallSearchFreeAgent) {
                        messagingTemplate.convertAndSend("/topic/" + userId, new Message(userId, "Wait",
                                TypeOfMessage.CallSearchFreeAgent, null, null));
                    } else messagingTemplate.convertAndSend("/topic/" + userId, new Message(userId,
                            "Нет свободных агентов, ожидайте. Все ваши уже написанные сообщения будут отправлены подключенному агенту",
                            TypeOfMessage.NoFreeAgent, null, null));
                }
            }

        } else {
            if (firstMessage.getTo() == null) {
                messagingTemplate.convertAndSend("/topic/" + userId, new Message(userId, "Вы не можете начать диалог, дождитесь подключения клиента",
                        TypeOfMessage.FirstMessageAgent, null, null));
            }
        }
    }


    public void setFirstMessage(Message firstMessage) {
        this.firstMessage = firstMessage;
    }

    public void setHeaderAccessor(SimpMessageHeaderAccessor headerAccessor) {
        this.headerAccessor = headerAccessor;
    }

    public void setMessagingTemplate(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}
