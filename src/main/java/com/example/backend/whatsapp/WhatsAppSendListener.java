package com.example.backend.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RabbitTemplate.class)
public class WhatsAppSendListener {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSendListener.class);

    private final WhatsAppCloudClient whatsAppCloudClient;
    private final WhatsAppPublisher whatsAppPublisher;

    public WhatsAppSendListener(WhatsAppCloudClient whatsAppCloudClient, WhatsAppPublisher whatsAppPublisher) {
        this.whatsAppCloudClient = whatsAppCloudClient;
        this.whatsAppPublisher = whatsAppPublisher;
    }

    @RabbitListener(queues = WhatsAppQueueNames.QUEUE_SEND)
    public void onWhatsAppSend(WhatsAppJob job) {
        boolean sent = whatsAppCloudClient.send(job);
        if (sent) {
            return;
        }
        if (job.canRetry()) {
            WhatsAppJob next = job.nextAttempt();
            log.warn(
                "Scheduling WhatsApp retry in {} ms: to={} template={} nextAttempt={}",
                WhatsAppQueueNames.RETRY_DELAY_MS,
                next.toE164(),
                next.templateName(),
                next.attempt());
            whatsAppPublisher.publishRetry(next);
            return;
        }
        log.error(
            "WhatsApp moved to DLQ after {} attempts: to={} template={}",
            job.attempt() + 1,
            job.toE164(),
            job.templateName());
        whatsAppPublisher.publishDlq(job);
    }
}
