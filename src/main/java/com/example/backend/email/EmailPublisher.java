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
    private final EmailOutboxService emailOutboxService;

    public EmailPublisher(
        ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
        SmtpEmailSender smtpEmailSender,
        EmailOutboxService emailOutboxService) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.smtpEmailSender = smtpEmailSender;
        this.emailOutboxService = emailOutboxService;
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
            boolean sent = smtpEmailSender.send(job);
            if (!sent) {
                emailOutboxService.enqueueFailed(job, "SMTP send failed (direct, no RabbitMQ)");
            }
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
            log.warn(
                "Cannot schedule Rabbit retry; persisting to email outbox: to={} subject={} attempt={}",
                job.to(),
                job.subject(),
                job.attempt());
            emailOutboxService.enqueueFailed(job, "Retry requested without RabbitMQ");
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
                "Cannot dead-letter email without RabbitMQ; marking outbox DEAD: to={} subject={} attempt={}",
                job.to(),
                job.subject(),
                job.attempt());
            // Force dead by using attempt at/above max
            EmailJob dead = new EmailJob(
                job.to(),
                job.subject(),
                job.body(),
                EmailJob.MAX_RETRY_ATTEMPTS,
                job.attachments());
            emailOutboxService.enqueueFailed(dead, "Moved to DLQ without RabbitMQ");
            return;
        }
        rabbitTemplate.convertAndSend(
            EmailQueueNames.EXCHANGE,
            EmailQueueNames.ROUTING_DLQ,
            job);
    }
}
