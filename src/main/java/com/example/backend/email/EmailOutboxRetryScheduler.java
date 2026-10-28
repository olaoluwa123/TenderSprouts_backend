package com.example.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailOutboxRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxRetryScheduler.class);

    private final EmailOutboxService emailOutboxService;

    public EmailOutboxRetryScheduler(EmailOutboxService emailOutboxService) {
        this.emailOutboxService = emailOutboxService;
    }

    @Scheduled(fixedDelayString = "${app.mail.outbox-retry-delay-ms:60000}")
    public void retryPending() {
        int sent = emailOutboxService.retryDue();
        if (sent > 0) {
            log.info("Email outbox retry sent {} message(s)", sent);
        }
    }
}
