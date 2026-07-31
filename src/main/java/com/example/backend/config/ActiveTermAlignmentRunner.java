package com.example.backend.config;

import com.example.backend.service.ActiveTermService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ActiveTermAlignmentRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ActiveTermAlignmentRunner.class);

    private final ActiveTermService activeTermService;

    public ActiveTermAlignmentRunner(ActiveTermService activeTermService) {
        this.activeTermService = activeTermService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            activeTermService.alignActiveTermWithActiveSession();
        } catch (Exception ex) {
            log.debug("Skipping active term alignment: {}", ex.getMessage());
        }
    }
}
