package com.example.backend.importhub;

import com.example.backend.service.StudentCsvImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RabbitTemplate.class)
public class StudentCsvImportListener {

    private static final Logger log = LoggerFactory.getLogger(StudentCsvImportListener.class);

    private final StudentCsvImportService studentCsvImportService;

    public StudentCsvImportListener(StudentCsvImportService studentCsvImportService) {
        this.studentCsvImportService = studentCsvImportService;
    }

    @RabbitListener(queues = ImportQueueNames.QUEUE_STUDENTS)
    public void onImportStudents(ImportStudentsJob job) {
        if (job == null || job.jobId() == null) {
            log.error("Ignoring invalid student CSV import message");
            return;
        }
        try {
            studentCsvImportService.executeJob(job.jobId());
        } catch (Exception ex) {
            log.error("Student CSV import job failed unexpectedly: jobId={}", job.jobId(), ex);
            studentCsvImportService.markFailed(job.jobId(), ex.getMessage());
        }
    }
}
