package com.planok.ssewhatsapp.consumer;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

public interface ConsumerInterface {

    void receiveMessage(Message message, Channel channel, long tag, Integer retryCount);
}
