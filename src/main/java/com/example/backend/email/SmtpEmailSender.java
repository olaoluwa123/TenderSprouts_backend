package com.example.backend.email;

import com.example.backend.config.AppMailProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppMailProperties mailProperties;

    public SmtpEmailSender(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        AppMailProperties mailProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
    }

    /**
     * @return {@code true} when the job was handled (sent or intentionally skipped);
     *         {@code false} when SMTP was enabled and sending failed
     */
    public boolean send(EmailJob job) {
        if (!mailProperties.isEnabled()) {
            log.info(
                "Email skipped (mail disabled): to={} subject={} attachments={}\n{}",
                job.to(),
                job.subject(),
                job.hasAttachments() ? job.attachments().size() : 0,
                job.body());
            return true;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn(
                "Mail enabled but JavaMailSender not configured; logging email to={} subject={}",
                job.to(),
                job.subject());
            log.info("{}", job.body());
            return true;
        }
        try {
            boolean multipart = job.hasAttachments();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(job.to());
            helper.setSubject(job.subject());
            helper.setText(job.body(), false);
            if (multipart) {
                for (EmailAttachment attachment : job.attachments()) {
                    helper.addAttachment(
                        attachment.filename(),
                        new ByteArrayResource(attachment.content()),
                        attachment.contentType());
                }
            }
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.error(
                "Failed to send email to={} subject={} attempt={}",
                job.to(),
                job.subject(),
                job.attempt(),
                ex);
            return false;
        }
    }
}
