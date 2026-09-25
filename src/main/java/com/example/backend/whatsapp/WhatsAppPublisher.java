package com.example.backend.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class WhatsAppPublisher {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final WhatsAppCloudClient whatsAppCloudClient;

    public WhatsAppPublisher(
        ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
        WhatsAppCloudClient whatsAppCloudClient) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.whatsAppCloudClient = whatsAppCloudClient;
    }

    public void publish(WhatsAppJob job) {
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

    private void dispatch(WhatsAppJob job) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.debug(
                "RabbitTemplate unavailable; sending WhatsApp directly after commit to={} template={}",
                job.toE164(),
                job.templateName());
            whatsAppCloudClient.send(job);
            return;
        }
        rabbitTemplate.convertAndSend(
            WhatsAppQueueNames.EXCHANGE,
            WhatsAppQueueNames.ROUTING_SEND,
            job);
    }

    public void publishRetry(WhatsAppJob job) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.error(
                "Cannot schedule WhatsApp retry without RabbitMQ: to={} template={} attempt={}",
                job.toE164(),
                job.templateName(),
                job.attempt());
            return;
        }
        rabbitTemplate.convertAndSend(
            WhatsAppQueueNames.EXCHANGE,
            WhatsAppQueueNames.ROUTING_RETRY,
            job);
    }

    public void publishDlq(WhatsAppJob job) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.error(
                "Cannot dead-letter WhatsApp without RabbitMQ: to={} template={} attempt={}",
                job.toE164(),
                job.templateName(),
                job.attempt());
            return;
        }
        rabbitTemplate.convertAndSend(
            WhatsAppQueueNames.EXCHANGE,
            WhatsAppQueueNames.ROUTING_DLQ,
            job);
    }
}
