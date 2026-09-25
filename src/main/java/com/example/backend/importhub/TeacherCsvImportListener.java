package com.example.backend.importhub;

import com.example.backend.service.TeacherCsvImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RabbitTemplate.class)
public class TeacherCsvImportListener {

    private static final Logger log = LoggerFactory.getLogger(TeacherCsvImportListener.class);

    private final TeacherCsvImportService teacherCsvImportService;

    public TeacherCsvImportListener(TeacherCsvImportService teacherCsvImportService) {
        this.teacherCsvImportService = teacherCsvImportService;
    }

    @RabbitListener(queues = ImportQueueNames.QUEUE_TEACHERS)
    public void onImportTeachers(ImportTeachersJob job) {
        if (job == null || job.jobId() == null) {
            log.error("Ignoring invalid teacher CSV import message");
            return;
        }
        try {
            teacherCsvImportService.executeJob(job.jobId());
        } catch (Exception ex) {
            log.error("Teacher CSV import job failed unexpectedly: jobId={}", job.jobId(), ex);
            teacherCsvImportService.markFailed(job.jobId(), ex.getMessage());
        }
    }
}
