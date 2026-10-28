package com.example.backend.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.config.AppMailProperties;
import com.example.backend.entity.EmailOutbox;
import com.example.backend.entity.enums.EmailOutboxStatus;
import com.example.backend.repository.EmailOutboxRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

class EmailOutboxServiceTest {

    private EmailOutboxRepository repository;
    private final List<EmailOutbox> saved = new ArrayList<>();
    private AtomicBoolean sendSucceeds;
    private EmailOutboxService service;

    @BeforeEach
    void setUp() {
        saved.clear();
        sendSucceeds = new AtomicBoolean(false);
        repository = Mockito.mock(EmailOutboxRepository.class);
        Mockito.when(repository.save(Mockito.any(EmailOutbox.class))).thenAnswer(inv -> {
            EmailOutbox row = inv.getArgument(0);
            if (row.getId() == null) {
                row.setId((long) (saved.size() + 1));
            }
            saved.removeIf(existing -> existing.getId() != null && existing.getId().equals(row.getId()));
            saved.add(row);
            return row;
        });

        SmtpEmailSender sender = new SmtpEmailSender(nullProvider(), mailEnabled()) {
            @Override
            public boolean send(EmailJob job) {
                return sendSucceeds.get();
            }
        };
        service = new EmailOutboxService(repository, sender);
    }

    @Test
    void enqueueFailedPersistsPendingWhenRetriesRemain() {
        EmailJob job = EmailJob.initial("a@b.com", "Hello", "Body");
        service.enqueueFailed(job, "SMTP failed");

        assertEquals(1, saved.size());
        EmailOutbox row = saved.get(0);
        assertEquals(EmailOutboxStatus.PENDING, row.getStatus());
        assertEquals("a@b.com", row.getToEmail());
        assertEquals("Hello", row.getSubject());
        assertEquals("Body", row.getBody());
        assertEquals(0, row.getAttempt());
        assertTrue(row.getNextAttemptAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void enqueueFailedMarksDeadWhenAttemptsExhausted() {
        EmailJob job = new EmailJob("a@b.com", "Hello", "Body", EmailJob.MAX_RETRY_ATTEMPTS, List.of());
        service.enqueueFailed(job, "gave up");

        assertEquals(1, saved.size());
        assertEquals(EmailOutboxStatus.DEAD, saved.get(0).getStatus());
        assertNull(saved.get(0).getNextAttemptAt());
    }

    @Test
    void retryDueMarksSentOnSuccess() {
        EmailOutbox pending = pendingRow(0);
        Mockito.when(repository.findDue(
                Mockito.eq(EmailOutboxStatus.PENDING),
                Mockito.any(LocalDateTime.class),
                Mockito.any(Pageable.class)))
            .thenReturn(List.of(pending));
        sendSucceeds.set(true);

        int sent = service.retryDue();

        assertEquals(1, sent);
        assertEquals(EmailOutboxStatus.SENT, pending.getStatus());
        assertTrue(pending.getSentAt() != null);
    }

    @Test
    void retryDueMarksDeadAfterMaxAttempts() {
        EmailOutbox pending = pendingRow(EmailJob.MAX_RETRY_ATTEMPTS - 1);
        Mockito.when(repository.findDue(
                Mockito.eq(EmailOutboxStatus.PENDING),
                Mockito.any(LocalDateTime.class),
                Mockito.any(Pageable.class)))
            .thenReturn(List.of(pending));
        sendSucceeds.set(false);

        int sent = service.retryDue();

        assertEquals(0, sent);
        assertEquals(EmailOutboxStatus.DEAD, pending.getStatus());
        assertEquals(EmailJob.MAX_RETRY_ATTEMPTS, pending.getAttempt());
        assertNull(pending.getNextAttemptAt());
    }

    @Test
    void publisherPersistsOutboxWhenDirectSendFails() {
        List<EmailJob> failed = new ArrayList<>();
        EmailOutboxService outbox = new EmailOutboxService(repository, new SmtpEmailSender(nullProvider(), mailEnabled())) {
            @Override
            public void enqueueFailed(EmailJob job, String error) {
                failed.add(job);
            }
        };
        SmtpEmailSender failingSender = new SmtpEmailSender(nullProvider(), mailEnabled()) {
            @Override
            public boolean send(EmailJob job) {
                return false;
            }
        };
        EmailPublisher publisher = new EmailPublisher(nullProvider(), failingSender, outbox);
        publisher.publish(EmailJob.initial("x@y.com", "S", "B"));

        assertEquals(1, failed.size());
        assertEquals("x@y.com", failed.get(0).to());
    }

    private static EmailOutbox pendingRow(int attempt) {
        EmailOutbox row = new EmailOutbox();
        row.setId(1L);
        row.setToEmail("a@b.com");
        row.setSubject("Hello");
        row.setBody("Body");
        row.setAttempt(attempt);
        row.setStatus(EmailOutboxStatus.PENDING);
        row.setNextAttemptAt(LocalDateTime.now().minusMinutes(1));
        return row;
    }

    private static AppMailProperties mailEnabled() {
        AppMailProperties props = new AppMailProperties();
        props.setEnabled(true);
        props.setFrom("noreply@school.local");
        return props;
    }

    private static <T> ObjectProvider<T> nullProvider() {
        return new ObjectProvider<>() {
            @Override
            public T getObject() throws BeansException {
                return null;
            }

            @Override
            public T getObject(Object... args) throws BeansException {
                return null;
            }

            @Override
            public T getIfAvailable() throws BeansException {
                return null;
            }

            @Override
            public T getIfUnique() throws BeansException {
                return null;
            }
        };
    }
}
