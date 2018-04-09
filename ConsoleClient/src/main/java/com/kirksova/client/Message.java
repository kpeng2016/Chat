package com.kirksova.client;

public class Message {

    private MessageType typeOfMessage;
    private Long senderId;
    private Long to;
    private String senderName;
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
        this.senderName = nameTo;
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

    public String getSenderName() {
        return senderName;
    }

    public enum MessageType {
        LEAVE_CLIENT, INCORRECT_REGISTRATION_DATA, CORRECT_REGISTRATION, CONNECTED_CLIENT, CONNECTED_AGENT,
        NO_FREE_AGENT, FIRST_MESSAGE_AGENT, END_DIALOGUE, DISCONNECTION_OF_THE_CLIENT, DISCONNECTION_OF_THE_AGENT,
        MESSAGE_CHAT, NO_CLIENT_IN_QUEUE, INCORRECT_LOGIN_NAME, CORRECT_LOGIN_NAME, INCORRECT_LOGIN_PASSWORD,
        CORRECT_LOGIN_PASSWORD, INCORRECT_DATA_MAX_COUNT_CLIENTS, CORRECT_DATA_MAX_COUNT_CLIENTS, AGENT_CANT_LEAVE,
    }
}
