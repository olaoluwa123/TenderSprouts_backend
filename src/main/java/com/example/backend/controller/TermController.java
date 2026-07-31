package com.example.backend.controller;

import com.example.backend.dto.session.TermResponse;
import com.example.backend.service.ActiveTermService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/terms")
public class TermController {

    private final ActiveTermService activeTermService;

    public TermController(ActiveTermService activeTermService) {
        this.activeTermService = activeTermService;
    }

    @GetMapping("/active")
    public TermResponse getActive() {
        return TermResponse.from(activeTermService.requireActiveTerm());
    }
}
