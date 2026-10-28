package com.example.backend.email;

import com.example.backend.entity.EmailOutbox;
import com.example.backend.entity.enums.EmailOutboxStatus;
import com.example.backend.repository.EmailOutboxRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOutboxService {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);
    private static final int BATCH_SIZE = 50;

    private final EmailOutboxRepository emailOutboxRepository;
    private final SmtpEmailSender smtpEmailSender;

    public EmailOutboxService(
        EmailOutboxRepository emailOutboxRepository,
        SmtpEmailSender smtpEmailSender) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.smtpEmailSender = smtpEmailSender;
    }

    @Transactional
    public void enqueueFailed(EmailJob job, String error) {
        EmailOutbox row = new EmailOutbox();
        row.setToEmail(job.to());
        row.setSubject(job.subject() == null ? "" : truncate(job.subject(), 500));
        row.setBody(job.body() == null ? "" : job.body());
        row.setAttachmentsJson(EmailAttachmentCodec.toJson(job.attachments()));
        row.setAttempt(job.attempt());
        row.setLastError(truncate(error, 500));
        if (job.canRetry()) {
            row.setStatus(EmailOutboxStatus.PENDING);
            row.setNextAttemptAt(LocalDateTime.now().plusSeconds(EmailQueueNames.RETRY_DELAY_MS / 1000L));
        } else {
            row.setStatus(EmailOutboxStatus.DEAD);
            row.setNextAttemptAt(null);
        }
        emailOutboxRepository.save(row);
        log.warn(
            "Email persisted to outbox: id={} to={} subject={} status={} attempt={}",
            row.getId(),
            row.getToEmail(),
            row.getSubject(),
            row.getStatus(),
            row.getAttempt());
    }

    @Transactional
    public int retryDue() {
        LocalDateTime now = LocalDateTime.now();
        List<EmailOutbox> due = emailOutboxRepository.findDue(
            EmailOutboxStatus.PENDING,
            now,
            PageRequest.of(0, BATCH_SIZE));
        int sent = 0;
        for (EmailOutbox row : due) {
            EmailJob job = toJob(row);
            boolean ok = smtpEmailSender.send(job);
            if (ok) {
                row.setStatus(EmailOutboxStatus.SENT);
                row.setSentAt(LocalDateTime.now());
                row.setLastError(null);
                row.setNextAttemptAt(null);
                sent++;
            } else {
                EmailJob next = job.nextAttempt();
                row.setAttempt(next.attempt());
                row.setLastError(truncate("SMTP send failed on retry", 500));
                if (next.canRetry()) {
                    row.setStatus(EmailOutboxStatus.PENDING);
                    row.setNextAttemptAt(
                        LocalDateTime.now().plusSeconds(EmailQueueNames.RETRY_DELAY_MS / 1000L));
                } else {
                    row.setStatus(EmailOutboxStatus.DEAD);
                    row.setNextAttemptAt(null);
                }
            }
            emailOutboxRepository.save(row);
        }
        return sent;
    }

    private static EmailJob toJob(EmailOutbox row) {
        return new EmailJob(
            row.getToEmail(),
            row.getSubject(),
            row.getBody(),
            row.getAttempt(),
            EmailAttachmentCodec.fromJson(row.getAttachmentsJson()));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
