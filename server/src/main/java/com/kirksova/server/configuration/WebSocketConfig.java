package com.kirksova.server.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@PropertySource(value = "classpath:configuration.properties", encoding = "UTF-8")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${host.topic}")
    private String topic;
    @Value("${host.app}")
    private String app;
    @Value("${host.ws}")
    private String ws;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(topic);
        config.setApplicationDestinationPrefixes(app);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ws);
        registry.addEndpoint(ws).withSockJS();
    }
}
