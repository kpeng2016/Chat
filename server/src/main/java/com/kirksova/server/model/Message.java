package com.kirksova.server.model;

public class Message {

    private MessageType typeOfMessage;
    private Long senderId;
    private Long to;
    private String nameTo;
    private String text;

    public Message() {
    }

    public Message(Long senderId, String text, MessageType typeOfMessage) {
         this(senderId, text, typeOfMessage, null, null);
    }

    public Message(Long senderId, String text, MessageType typeOfMessage, Long to, String nameTo) {
        this.senderId = senderId;
        this.text = text;
        this.typeOfMessage = typeOfMessage;
        this.to = to;
        this.nameTo = nameTo;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getText() {
        return text;
    }

    public MessageType getTypeOfMessage() {
        return typeOfMessage;
    }

    public Long getTo() {
        return to;
    }

    public String getNameTo() {
        return nameTo;
    }

    public enum MessageType {
        LEAVE_CLIENT, AGENT_CANT_LEAVE, NOT_VALID_REGISTRATION_DATA, CORRECT_REGISTRATION,
        CONNECTED_CLIENT, CONNECTED_AGENT, NO_FREE_AGENT, FIRST_MESSAGE_AGENT, END_DIALOGUE,
        DISCONNECTION_OF_THE_CLIENT, DISCONNECTION_OF_THE_AGENT, YOUR_MESSAGES, CALL_SEARCH_FREE_AGENT,
        REGISTER, MESSAGE_CHAT,
    }

    public void setTypeOfMessage(MessageType typeOfMessage) {
        this.typeOfMessage = typeOfMessage;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public void setTo(Long to) {
        this.to = to;
    }

    public void setNameTo(String nameTo) {
        this.nameTo = nameTo;
    }

    public void setText(String text) {
        this.text = text;
    }
}
