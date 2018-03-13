package com.kirksova.server.service;

import com.kirksova.server.model.Message;
import com.kirksova.server.model.User;
import com.kirksova.server.model.enumType.TypeOfMessage;
import com.kirksova.server.model.enumType.TypeOfUser;
import org.apache.log4j.Logger;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
public class LeaveUser {

    private static final Logger log = Logger.getLogger(LeaveUser.class);
    private SimpMessageHeaderAccessor headerAccessor;
    private Message messageLeave;
    private SimpMessageSendingOperations messagingTemplate;

    public void checkValidLeave() {
        User user = (User) headerAccessor.getSessionAttributes().get("User");
        User interlocutor = (User) headerAccessor.getSessionAttributes().get("Interlocutor");
        if (messageLeave.getTo() != null) {
            if (user.getUserType() == TypeOfUser.client) {
                log.info("Dialogue between agent " + messageLeave.getNameTo() + " and client " + user.getName() + " was over");
                Message chatMessage = new Message(messageLeave.getTo(), "Диалог завершен",
                        TypeOfMessage.EndDialogue, null, null);
                messagingTemplate.convertAndSend("/topic/" + messageLeave.getTo(), chatMessage);
                chatMessage = new Message(messageLeave.getTo(), "Клиент вышел из чата для закрытия приложения используйте /exit, " +
                        "или дождитесь подключения нового клиента", TypeOfMessage.LeaveClient, null, null);
                messagingTemplate.convertAndSend("/topic/" + messageLeave.getTo(), chatMessage);
                interlocutor.setFreeAgent(true);
                messagingTemplate.convertAndSend("/topic/" + user.getId(), new Message(user.getId(), "Диалог завершен", TypeOfMessage.EndDialogue, null, null));
            } else
                messagingTemplate.convertAndSend("/topic/" + user.getId(), new Message(user.getId(), "Агент не может выйти из чата",
                        TypeOfMessage.AgentCantLeave, messageLeave.getTo(), messageLeave.getNameTo()));
        } else
            messagingTemplate.convertAndSend("/topic/" + user.getId(), new Message(user.getId(), "У вас сейчас нет активного диалога",
                    TypeOfMessage.EndDialogue, null, null));
    }

    public void setMessageLeave(Message messageLeave) {
        this.messageLeave = messageLeave;
    }

    public void setHeaderAccessor(SimpMessageHeaderAccessor headerAccessor) {
        this.headerAccessor = headerAccessor;
    }

    public void setMessagingTemplate(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}
