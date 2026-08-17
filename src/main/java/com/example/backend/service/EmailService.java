package com.example.backend.service;

import com.example.backend.config.AppMailProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppMailProperties mailProperties;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider, AppMailProperties mailProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
    }

    public void sendWelcomeCredentials(
        String toEmail,
        String recipientName,
        String roleLabel,
        String temporaryPassword,
        String loginUrl) {
        String greeting = recipientName != null && !recipientName.isBlank()
            ? "Dear " + recipientName + ","
            : "Hello,";
        String roleDisplay = roleLabel != null && !roleLabel.isBlank()
            ? roleLabel.trim().toLowerCase()
            : "staff";
        String subject = "Welcome to Tender Sprouts - your portal login";
        String body = """
            %s

            Welcome to the Tender Sprouts School portal.

            An administrator has set up your %s account so you can access the school system online.

            --------------------------------
            Your sign-in details
            --------------------------------
            Email: %s
            Temporary password: %s

            Sign in here:
            %s

            Important: on your first sign-in you will be asked to choose a new password.
            You must complete that step before using the portal.

            Keep this temporary password private. Do not share it with anyone.

            If you did not expect this email, please contact the school office and ignore these details.

            Warm regards,
            Tender Sprouts School
            """.formatted(greeting, roleDisplay, toEmail, temporaryPassword, loginUrl);
        send(toEmail, subject, body.trim());
    }

    public void sendPasswordReset(String toEmail, String recipientName, String resetUrl) {
        String greeting = recipientName != null && !recipientName.isBlank()
            ? "Dear " + recipientName + ","
            : "Hello,";
        String subject = "Set your school portal password";
        String body = """
            %s

            An administrator has created your school portal account. Please set your password using the link below (valid for 24 hours):

            %s

            If you did not expect this email, contact the school office.

            Thank you.
            """.formatted(greeting, resetUrl);
        send(toEmail, subject, body.trim());
    }

    public void sendForgotPasswordReset(String toEmail, String recipientName, String resetUrl) {
        String greeting = recipientName != null && !recipientName.isBlank()
            ? "Dear " + recipientName + ","
            : "Hello,";
        String subject = "Reset your school portal password";
        String body = """
            %s

            We received a request to reset your school portal password. Use the link below to choose a new password (valid for 24 hours):

            %s

            If you did not request this, you can ignore this email. Your password will not change until you use the link above.

            Thank you.
            """.formatted(greeting, resetUrl);
        send(toEmail, subject, body.trim());
    }

    public void sendTermResultsPublished(
        String toEmail,
        String parentName,
        String childNames,
        String portalUrl) {
        String greeting = parentName != null && !parentName.isBlank()
            ? "Dear " + parentName + ","
            : "Dear parent,";
        String subject = "Term results approved";
        String body = """
            %s

            Term results have been approved and are now available for: %s.

            Sign in to view results and download report cards:
            %s

            Thank you.
            """.formatted(greeting, childNames, portalUrl);
        send(toEmail, subject, body.trim());
    }

    public void sendExamTimetablePublished(
        String toEmail,
        String parentName,
        String className,
        String portalUrl) {
        String greeting = parentName != null && !parentName.isBlank()
            ? "Dear " + parentName + ","
            : "Dear parent,";
        String subject = "Exam timetable published — " + className;
        String body = """
            %s

            The exam timetable for %s has been published.

            View the schedule in the parent portal:
            %s

            Thank you.
            """.formatted(greeting, className, portalUrl);
        send(toEmail, subject, body.trim());
    }

    @Async
    public void send(String to, String subject, String body) {
        if (!mailProperties.isEnabled()) {
            log.info("Email skipped (mail disabled): to={} subject={}\n{}", to, subject, body);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail enabled but JavaMailSender not configured; logging email to={} subject={}", to, subject);
            log.info("{}", body);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email to {}", to, ex);
        }
    }
}
