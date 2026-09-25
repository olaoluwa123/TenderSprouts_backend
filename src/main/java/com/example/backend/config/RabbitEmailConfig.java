package com.example.backend.config;

import com.example.backend.email.EmailQueueNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(ConnectionFactory.class)
public class RabbitEmailConfig {

    @Bean
    public MessageConverter emailMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EmailQueueNames.EXCHANGE, true, false);
    }

    @Bean
    public Queue emailSendQueue() {
        return QueueBuilder.durable(EmailQueueNames.QUEUE_SEND).build();
    }

    @Bean
    public Queue emailRetryQueue() {
        return QueueBuilder.durable(EmailQueueNames.QUEUE_RETRY)
            .withArgument("x-message-ttl", EmailQueueNames.RETRY_DELAY_MS)
            .withArgument("x-dead-letter-exchange", EmailQueueNames.EXCHANGE)
            .withArgument("x-dead-letter-routing-key", EmailQueueNames.ROUTING_SEND)
            .build();
    }

    @Bean
    public Queue emailDlqQueue() {
        return QueueBuilder.durable(EmailQueueNames.QUEUE_DLQ).build();
    }

    @Bean
    public Binding emailSendBinding(Queue emailSendQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailSendQueue).to(emailExchange).with(EmailQueueNames.ROUTING_SEND);
    }

    @Bean
    public Binding emailRetryBinding(Queue emailRetryQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailRetryQueue).to(emailExchange).with(EmailQueueNames.ROUTING_RETRY);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlqQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailDlqQueue).to(emailExchange).with(EmailQueueNames.ROUTING_DLQ);
    }
}
