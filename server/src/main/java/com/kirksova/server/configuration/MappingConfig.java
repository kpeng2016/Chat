package com.kirksova.server.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MappingConfig implements WebMvcConfigurer {

    @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            registry.addViewController("/client").setViewName("client.html");
            registry.addViewController("/agent").setViewName("agent.html");
        }

}
