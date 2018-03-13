package com.kirksova.server.model;

import com.kirksova.server.model.enumType.TypeOfMessage;

public class Message {

    private TypeOfMessage typeOfMessage;
    private Long senderId;
    private Long to;
    private String nameTo;
    private String text;

    public Message(Long senderId, String text, TypeOfMessage typeOfMessage, Long to, String nameTo) {
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

    public TypeOfMessage getTypeOfMessage() {
        return typeOfMessage;
    }

    public Long getTo() {
        return to;
    }

    public String getNameTo() {
        return nameTo;
    }
}
