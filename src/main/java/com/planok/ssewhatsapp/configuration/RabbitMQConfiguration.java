package com.planok.ssewhatsapp.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMQConfiguration {

    @Getter
    @Value("${app.rabbitmq.queue.name}")
    public String primaryQueueName;

    @Value("${app.rabbitmq.queue.fall_back.name}")
    public String fallBackQueueName;

    @Value("${app.rabbitmq.xchange.fall_back.name}")
    public String xchangeFallBackName;

    @Value("${app.rabbitmq.routing_key.fall_back.name}")
    public String routingKeyFallBackName;

    @Getter
    @Value("${app.rabbitmq.interceptor.max_attempts:5}")
    public int maxRetryAttempts;

    @Value("${app.rabbitmq.interceptor.initial_interval:2000}")
    public int initialInterval;

    @Value("${app.rabbitmq.interceptor.multiplier:2.0}")
    public double multiplier;

    @Value("${app.rabbitmq.interceptor.max_interval:10000}")
    public int maxInterval;

    private final CachingConnectionFactory cachingConnectionFactory;

    public RabbitMQConfiguration(CachingConnectionFactory cachingConnectionFactory) {
        this.cachingConnectionFactory = cachingConnectionFactory;
    }

    @Bean
    public Queue createQueueMessageProcessed() {
        return QueueBuilder
                .durable(primaryQueueName)
                .withArgument("x-dead-letter-exchange", xchangeFallBackName)
                .withArgument("x-dead-letter-routing-key", routingKeyFallBackName)
                .build();
    }

    @Bean
    public Declarables createDeadLetterQueueMessageProcessedSchema() {
        return new Declarables(
                new DirectExchange(xchangeFallBackName),
                new Queue(fallBackQueueName),
                new Binding(
                        fallBackQueueName,
                        Binding.DestinationType.QUEUE,
                        xchangeFallBackName,
                        routingKeyFallBackName,
                        null
                )
        );
    }

    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder
                .stateless()
                .maxAttempts(maxRetryAttempts)
                .backOffOptions(
                        initialInterval,
                        multiplier,
                        maxInterval
                )
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(xchangeFallBackName);
    }

    @Bean
    public Queue fallBackQueue() {
        return QueueBuilder.durable(fallBackQueueName).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(fallBackQueue())
                .to(deadLetterExchange())
                .with(routingKeyFallBackName);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, cachingConnectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    @Bean
    public Jackson2JsonMessageConverter converter(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return rabbitTemplate;
    }
}