package com.example.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RabbitTemplate.class)
public class EmailSendListener {

    private static final Logger log = LoggerFactory.getLogger(EmailSendListener.class);

    private final SmtpEmailSender smtpEmailSender;
    private final EmailPublisher emailPublisher;

    public EmailSendListener(SmtpEmailSender smtpEmailSender, EmailPublisher emailPublisher) {
        this.smtpEmailSender = smtpEmailSender;
        this.emailPublisher = emailPublisher;
    }

    @RabbitListener(queues = EmailQueueNames.QUEUE_SEND)
    public void onEmailSend(EmailJob job) {
        boolean sent = smtpEmailSender.send(job);
        if (sent) {
            return;
        }
        if (job.canRetry()) {
            EmailJob next = job.nextAttempt();
            log.warn(
                "Scheduling email retry in {} ms: to={} subject={} nextAttempt={}",
                EmailQueueNames.RETRY_DELAY_MS,
                next.to(),
                next.subject(),
                next.attempt());
            emailPublisher.publishRetry(next);
            return;
        }
        log.error(
            "Email moved to DLQ after {} attempts: to={} subject={}",
            job.attempt() + 1,
            job.to(),
            job.subject());
        emailPublisher.publishDlq(job);
    }
}
