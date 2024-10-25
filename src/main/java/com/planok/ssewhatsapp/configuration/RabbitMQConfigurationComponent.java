package com.planok.ssewhatsapp.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConfigurationComponent {

    private final RabbitMQConfiguration rabbitMQConfiguration;

    private static RabbitMQConfigurationComponent instance;

    public RabbitMQConfigurationComponent(RabbitMQConfiguration rabbitMQConfiguration) {
        this.rabbitMQConfiguration = rabbitMQConfiguration;
    }

    @PostConstruct
    private void init() {
        instance = this;
    }

    public static int getMaxRetryAttempts() {
        return instance.rabbitMQConfiguration.getMaxRetryAttempts();
    }

    public static String getPrimaryQueueName() {
        return instance.rabbitMQConfiguration.getPrimaryQueueName();
    }
}
