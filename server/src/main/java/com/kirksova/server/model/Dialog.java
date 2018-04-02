package com.kirksova.server.model;

import org.springframework.stereotype.Component;

@Component
public class Dialog {

    private Long agent;
    private Long client;

    public Long getAgent() {
        return agent;
    }

    public void setAgent(Long agent) {
        this.agent = agent;
    }

    public Long getClient() {
        return client;
    }

    public void setClient(Long client) {
        this.client = client;
    }
}
