package com.example.backend.importhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class StudentCsvImportPublisher {

    private static final Logger log = LoggerFactory.getLogger(StudentCsvImportPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    public StudentCsvImportPublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
    }

    /**
     * @return {@code true} when the job was published to RabbitMQ
     */
    public boolean publish(Long jobId) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.debug("RabbitTemplate unavailable; student CSV import job {} will run synchronously", jobId);
            return false;
        }
        rabbitTemplate.convertAndSend(
            ImportQueueNames.EXCHANGE,
            ImportQueueNames.ROUTING_STUDENTS,
            new ImportStudentsJob(jobId));
        return true;
    }
}
