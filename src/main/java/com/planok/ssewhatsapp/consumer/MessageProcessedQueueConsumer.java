package com.planok.ssewhatsapp.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planok.ssewhatsapp.configuration.RabbitMQConfiguration;
import com.planok.ssewhatsapp.event.MessageEvent;
import com.planok.ssewhatsapp.exception.SerializerHandlerException;
import com.planok.ssewhatsapp.service.SseEmitterService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class MessageProcessedQueueConsumer implements ConsumerInterface {

    private static final String ERROR_PROCESSING_MESSAGE = "Error processing message";
    private static final String MESSAGE_SUCCESS = "Message successfully processed and acknowledged";
    private static final String RETRYING_MESSAGE = "Retrying message, attempt: {}";
    private static final String MAX_RETRIES_REACHED = "Max retry attempts reached. Sending message to dead-letter queue";
    private static final String ERROR_NACK = "Error during message NACK";

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    public MessageProcessedQueueConsumer(
            SseEmitterService sseEmitterService,
            ObjectMapper objectMapper
    ) {
        this.sseEmitterService = sseEmitterService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = {"${app.rabbitmq.queue.name}"}, ackMode = "MANUAL")
    public void receiveMessage(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(name = "x-retry-count", required = false) Integer retryCount
    ) {
        log.info("Consuming message from queue: {}", RabbitMQConfiguration.primaryQueueName);
        try {
            MessageProperties messageProperties = message.getMessageProperties();
            if (!"application/json".equals(messageProperties.getContentType())) {
                log.error("Invalid content_type: {}", messageProperties.getContentType());
                throw new IllegalArgumentException("Invalid content_type: " + messageProperties.getContentType());
            }
            String messageBody = new String(message.getBody());
            MessageEvent messageEvent = deserializeMessage(messageBody);
            sseEmitterService.sendMessage(messageEvent);
            channel.basicAck(tag, false);
            log.info(MESSAGE_SUCCESS);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing message", e);
            handleNackWithRetry(channel, tag, retryCount, e);
        } catch (Exception e) {
            log.error(ERROR_PROCESSING_MESSAGE, e);
            handleNackWithRetry(channel, tag, retryCount, e);
        }
    }

    private MessageEvent deserializeMessage(String messageBody) throws JsonProcessingException {
        log.info("Deserializing message {}", messageBody);
        return objectMapper.readValue(messageBody, MessageEvent.class);
    }

    private void handleNackWithRetry(Channel channel, long tag, Integer retryCount, Exception originalException) {
        retryCount = (retryCount == null) ? 0 : retryCount;
        retryCount++;
        if (retryCount > RabbitMQConfiguration.maxRetryAttempts) {
            log.error(MAX_RETRIES_REACHED);
            sendToDeadLetterQueue(channel, tag);
        } else {
            log.info(RETRYING_MESSAGE, retryCount);
            retryMessage(channel, tag);
        }
        throw new SerializerHandlerException(ERROR_PROCESSING_MESSAGE + ". Retry count: " + retryCount, originalException);
    }

    private void retryMessage(Channel channel, long tag) {
        try {
            channel.basicNack(tag, false, true);
        } catch (Exception e) {
            log.error(ERROR_NACK, e);
        }
    }

    private void sendToDeadLetterQueue(Channel channel, long tag) {
        try {
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            log.error(ERROR_NACK, e);
        }
    }
}
