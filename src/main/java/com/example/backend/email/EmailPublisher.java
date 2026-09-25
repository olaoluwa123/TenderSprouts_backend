package com.example.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EmailPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final SmtpEmailSender smtpEmailSender;

    public EmailPublisher(
        ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
        SmtpEmailSender smtpEmailSender) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.smtpEmailSender = smtpEmailSender;
    }

    public void publish(EmailJob job) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
            && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(job);
                }
            });
            return;
        }
        dispatch(job);
    }

    private void dispatch(EmailJob job) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.debug(
                "RabbitTemplate unavailable; sending email directly after commit to={} subject={}",
                job.to(),
                job.subject());
            smtpEmailSender.send(job);
            return;
        }
        rabbitTemplate.convertAndSend(
            EmailQueueNames.EXCHANGE,
            EmailQueueNames.ROUTING_SEND,
            job);
    }

    public void publishRetry(EmailJob job) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.error(
                "Cannot schedule email retry without RabbitMQ: to={} subject={} attempt={}",
                job.to(),
                job.subject(),
                job.attempt());
            return;
        }
        rabbitTemplate.convertAndSend(
            EmailQueueNames.EXCHANGE,
            EmailQueueNames.ROUTING_RETRY,
            job);
    }

    public void publishDlq(EmailJob job) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.error(
                "Cannot dead-letter email without RabbitMQ: to={} subject={} attempt={}",
                job.to(),
                job.subject(),
                job.attempt());
            return;
        }
        rabbitTemplate.convertAndSend(
            EmailQueueNames.EXCHANGE,
            EmailQueueNames.ROUTING_DLQ,
            job);
    }
}
