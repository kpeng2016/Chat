package com.kirksova.server.model;

import java.util.Objects;

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
        LEAVE_CLIENT, INCORRECT_REGISTRATION_DATA, CORRECT_REGISTRATION, CONNECTED_CLIENT, CONNECTED_AGENT,
        NO_FREE_AGENT, FIRST_MESSAGE_AGENT, END_DIALOGUE, DISCONNECTION_OF_THE_CLIENT, DISCONNECTION_OF_THE_AGENT,
        MESSAGE_CHAT, NO_CLIENT_IN_QUEUE, INCORRECT_LOGIN_NAME, CORRECT_LOGIN_NAME, INCORRECT_LOGIN_PASSWORD,
        CORRECT_LOGIN_PASSWORD, INCORRECT_DATA_MAX_COUNT_CLIENTS, CORRECT_DATA_MAX_COUNT_CLIENTS, AGENT_CANT_LEAVE,
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

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message message = (Message) o;
        return typeOfMessage == message.typeOfMessage &&
            Objects.equals(senderId, message.senderId) &&
            Objects.equals(to, message.to) &&
            Objects.equals(nameTo, message.nameTo) &&
            Objects.equals(text, message.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeOfMessage, senderId, to, nameTo, text);
    }

    @Override
    public String toString() {
        return "Message{" +
            "typeOfMessage=" + typeOfMessage +
            ", senderId=" + senderId +
            ", to=" + to +
            ", nameTo='" + nameTo + '\'' +
            ", text='" + text + '\'' +
            '}';
    }
}
