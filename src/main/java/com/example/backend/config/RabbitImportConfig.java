package com.example.backend.config;

import com.example.backend.importhub.ImportQueueNames;
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
public class RabbitImportConfig {

    @Bean
    public DirectExchange importExchange() {
        return new DirectExchange(ImportQueueNames.EXCHANGE, true, false);
    }

    @Bean
    public Queue importStudentsQueue() {
        return QueueBuilder.durable(ImportQueueNames.QUEUE_STUDENTS)
            .withArgument("x-dead-letter-exchange", ImportQueueNames.EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ImportQueueNames.ROUTING_STUDENTS_DLQ)
            .build();
    }

    @Bean
    public Queue importStudentsDlqQueue() {
        return QueueBuilder.durable(ImportQueueNames.QUEUE_STUDENTS_DLQ).build();
    }

    @Bean
    public Binding importStudentsBinding(Queue importStudentsQueue, DirectExchange importExchange) {
        return BindingBuilder.bind(importStudentsQueue)
            .to(importExchange)
            .with(ImportQueueNames.ROUTING_STUDENTS);
    }

    @Bean
    public Binding importStudentsDlqBinding(Queue importStudentsDlqQueue, DirectExchange importExchange) {
        return BindingBuilder.bind(importStudentsDlqQueue)
            .to(importExchange)
            .with(ImportQueueNames.ROUTING_STUDENTS_DLQ);
    }

    @Bean
    public Queue importTeachersQueue() {
        return QueueBuilder.durable(ImportQueueNames.QUEUE_TEACHERS)
            .withArgument("x-dead-letter-exchange", ImportQueueNames.EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ImportQueueNames.ROUTING_TEACHERS_DLQ)
            .build();
    }

    @Bean
    public Queue importTeachersDlqQueue() {
        return QueueBuilder.durable(ImportQueueNames.QUEUE_TEACHERS_DLQ).build();
    }

    @Bean
    public Binding importTeachersBinding(Queue importTeachersQueue, DirectExchange importExchange) {
        return BindingBuilder.bind(importTeachersQueue)
            .to(importExchange)
            .with(ImportQueueNames.ROUTING_TEACHERS);
    }

    @Bean
    public Binding importTeachersDlqBinding(Queue importTeachersDlqQueue, DirectExchange importExchange) {
        return BindingBuilder.bind(importTeachersDlqQueue)
            .to(importExchange)
            .with(ImportQueueNames.ROUTING_TEACHERS_DLQ);
    }
}
