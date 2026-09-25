package com.example.backend.service;

import com.example.backend.email.EmailAttachment;
import com.example.backend.email.EmailJob;
import com.example.backend.email.EmailPublisher;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailPublisher emailPublisher;

    public EmailService(EmailPublisher emailPublisher) {
        this.emailPublisher = emailPublisher;
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
        enqueue(toEmail, subject, body.trim());
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
        enqueue(toEmail, subject, body.trim());
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
        enqueue(toEmail, subject, body.trim());
    }

    public void sendFeesPublished(
        String toEmail,
        String parentName,
        String childNames,
        String className,
        String termName,
        String portalUrl) {
        String greeting = parentName != null && !parentName.isBlank()
            ? "Dear " + parentName + ","
            : "Dear parent,";
        String termLabel = termName != null && !termName.isBlank() ? termName : "this term";
        String classLabel = className != null && !className.isBlank() ? className : "their class";
        String children = childNames != null && !childNames.isBlank() ? childNames : "your child";
        String subject = "School fees published — " + termLabel;
        String body = """
            %s

            School fees for %s (%s) have been published and are now available on the parent portal.

            This notice covers: %s.

            Please sign in to view the fee breakdown, outstanding balance, and payment options:
            %s

            If you have questions about any charge, contact the school office.

            Warm regards,
            Tender Sprouts School
            """.formatted(greeting, termLabel, classLabel, children, portalUrl);
        enqueue(toEmail, subject, body.trim());
    }

    public void sendTermResultsPublished(
        String toEmail,
        String parentName,
        String childNames,
        String termName,
        String portalUrl,
        List<EmailAttachment> attachments) {
        String greeting = parentName != null && !parentName.isBlank()
            ? "Dear " + parentName + ","
            : "Dear parent,";
        String termLabel = termName != null && !termName.isBlank() ? termName : "this term";
        String children = childNames != null && !childNames.isBlank() ? childNames : "your child";
        String subject = "Term results ready — " + termLabel;
        String attachmentNote = attachments != null && !attachments.isEmpty()
            ? """

            We have attached the report card PDF(s) for your convenience. You can also download them any time from the portal.
            """
            : "\n";
        String body = """
            %s

            Term results for %s have been approved and are now available for: %s.
            %s
            Sign in to the parent portal to view results and download report cards:
            %s

            Warm regards,
            Tender Sprouts School
            """.formatted(greeting, termLabel, children, attachmentNote, portalUrl);
        enqueue(toEmail, subject, body.trim(), attachments);
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
        enqueue(toEmail, subject, body.trim());
    }

    private void enqueue(String to, String subject, String body) {
        enqueue(to, subject, body, List.of());
    }

    private void enqueue(String to, String subject, String body, List<EmailAttachment> attachments) {
        emailPublisher.publish(EmailJob.initial(to, subject, body, attachments));
    }
}
