package com.example.backend.config;

import com.example.backend.whatsapp.WhatsAppQueueNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(ConnectionFactory.class)
public class RabbitWhatsAppConfig {

    @Bean
    public DirectExchange whatsappExchange() {
        return new DirectExchange(WhatsAppQueueNames.EXCHANGE, true, false);
    }

    @Bean
    public Queue whatsappSendQueue() {
        return QueueBuilder.durable(WhatsAppQueueNames.QUEUE_SEND).build();
    }

    @Bean
    public Queue whatsappRetryQueue() {
        return QueueBuilder.durable(WhatsAppQueueNames.QUEUE_RETRY)
            .withArgument("x-message-ttl", WhatsAppQueueNames.RETRY_DELAY_MS)
            .withArgument("x-dead-letter-exchange", WhatsAppQueueNames.EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WhatsAppQueueNames.ROUTING_SEND)
            .build();
    }

    @Bean
    public Queue whatsappDlqQueue() {
        return QueueBuilder.durable(WhatsAppQueueNames.QUEUE_DLQ).build();
    }

    @Bean
    public Binding whatsappSendBinding(Queue whatsappSendQueue, DirectExchange whatsappExchange) {
        return BindingBuilder.bind(whatsappSendQueue).to(whatsappExchange).with(WhatsAppQueueNames.ROUTING_SEND);
    }

    @Bean
    public Binding whatsappRetryBinding(Queue whatsappRetryQueue, DirectExchange whatsappExchange) {
        return BindingBuilder.bind(whatsappRetryQueue).to(whatsappExchange).with(WhatsAppQueueNames.ROUTING_RETRY);
    }

    @Bean
    public Binding whatsappDlqBinding(Queue whatsappDlqQueue, DirectExchange whatsappExchange) {
        return BindingBuilder.bind(whatsappDlqQueue).to(whatsappExchange).with(WhatsAppQueueNames.ROUTING_DLQ);
    }
}
