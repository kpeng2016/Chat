package com.kirksova.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirksova.server.model.Message;
import com.kirksova.server.model.Message.MessageType;
import com.kirksova.server.model.User;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

@Service
public class MessageService {

    private static final Logger log = Logger.getLogger(MessageService.class);
    @Value("${host.topic}")
    private String topic;
    @Value("${message.agentCantLeave}")
    private String agentCantLeave;
    @Value("${message.noActiveDialogue}")
    private String noActiveDialogue;
    @Value("${message.disconnectedOfTheClient}")
    private String disconnectedOfTheClient;
    @Value("${message.endDialogue}")
    private String endDialogue;

    public void sendMessageToSocket(User interlocutor, Message message) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            OutputStream outMessageInterloc = interlocutor.getUserSocket().getOutputStream();
            PrintWriter outInterloc = new PrintWriter(new OutputStreamWriter(outMessageInterloc, "UTF-8"), true);
            outInterloc.println(mapper.writeValueAsString(message));
        } catch (IOException e) {
            log.debug("IOException ", e);
        }
    }

    public void sendMessageToWeb(User interlocutor, Message message) {
        SimpMessageSendingOperations messagingTemplate = interlocutor.getMessagingTemplate();
        messagingTemplate.convertAndSend(topic + interlocutor.getId(), message);
    }

    public Message checkValidLeave(User user, User interlocutor) {
        if (interlocutor != null) {
            if (user.getUserType() == User.TypeOfUser.CLIENT) {
                log.info(
                    "Dialogue between agent " + interlocutor.getName() + " and client " + user.getName() + " was over");
                interlocutor.setFreeAgent(true);
                return new Message(interlocutor.getId(), disconnectedOfTheClient, MessageType.LEAVE_CLIENT);
            } else {
                return new Message(user.getId(), agentCantLeave, MessageType.AGENT_CANT_LEAVE, interlocutor.getId(),
                    interlocutor.getName());
            }
        } else {
            return new Message(user.getId(), noActiveDialogue, MessageType.END_DIALOGUE);
        }
    }

    public Message endDialogMessage(Long id) {
        return new Message(id, endDialogue, MessageType.END_DIALOGUE);
    }

}
