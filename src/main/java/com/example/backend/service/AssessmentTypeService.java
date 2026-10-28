package com.example.backend.service;

import com.example.backend.dto.assessment.AssessmentTypeResponse;
import com.example.backend.dto.assessment.CreateAssessmentTypeRequest;
import com.example.backend.dto.assessment.UpdateAssessmentTypeRequest;
import com.example.backend.entity.AssessmentTypeLookup;
import com.example.backend.entity.User;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssessmentTypeLookupRepository;
import com.example.backend.security.SecurityUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentTypeService {

    private final AssessmentTypeLookupRepository assessmentTypeLookupRepository;
    private final AuditService auditService;

    public AssessmentTypeService(
        AssessmentTypeLookupRepository assessmentTypeLookupRepository,
        AuditService auditService) {
        this.assessmentTypeLookupRepository = assessmentTypeLookupRepository;
        this.auditService = auditService;
    }

    public List<AssessmentTypeResponse> list(SecurityUser actor) {
        if (actor.getRole() == User.Role.ADMIN) {
            return assessmentTypeLookupRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(AssessmentTypeResponse::from)
                .toList();
        }
        return listActive();
    }

    public List<AssessmentTypeResponse> listActive() {
        return assessmentTypeLookupRepository.findByIsActiveTrueOrderBySortOrderAscCodeAsc().stream()
            .map(AssessmentTypeResponse::from)
            .toList();
    }

    /** Active types with a positive max score — these become columns on grading/result sheets. */
    public List<AssessmentTypeLookup> listWeightedActiveEntities() {
        return assessmentTypeLookupRepository.findByIsActiveTrueOrderBySortOrderAscCodeAsc().stream()
            .filter(type -> type.getDefaultMaxScore() != null && type.getDefaultMaxScore().signum() > 0)
            .toList();
    }

    public AssessmentTypeLookup requireActiveType(String code) {
        String normalized = normalizeCode(code);
        AssessmentTypeLookup type = assessmentTypeLookupRepository.findByCode(normalized)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment type not found: " + normalized));
        if (!Boolean.TRUE.equals(type.getIsActive())) {
            throw new ConflictException("Assessment type is not active: " + normalized);
        }
        if (type.getDefaultMaxScore() == null || type.getDefaultMaxScore().signum() <= 0) {
            throw new ConflictException("Assessment type max score is not configured: " + normalized);
        }
        return type;
    }

    @Transactional
    public AssessmentTypeResponse create(CreateAssessmentTypeRequest request, SecurityUser actor) {
        requireAdmin(actor);
        String code = request.code() != null && !request.code().isBlank()
            ? normalizeCode(request.code())
            : generateUniqueCode(request.name());
        if (assessmentTypeLookupRepository.existsByCode(code)) {
            throw new ConflictException("Assessment type code already exists");
        }
        AssessmentTypeLookup type = new AssessmentTypeLookup();
        type.setCode(code);
        type.setName(request.name().trim());
        type.setDefaultMaxScore(request.defaultMaxScore());
        type.setWeight(BigDecimal.valueOf(100));
        type.setSortOrder(nextSortOrder());
        type.setIsActive(true);
        type = assessmentTypeLookupRepository.save(type);
        auditService.logAdminAction(actor, "CREATE", "assessment_type", type.getId(), type.getCode());
        return AssessmentTypeResponse.from(type);
    }

    @Transactional
    public AssessmentTypeResponse update(Long id, UpdateAssessmentTypeRequest request, SecurityUser actor) {
        requireAdmin(actor);
        AssessmentTypeLookup type = assessmentTypeLookupRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment type not found"));
        if (request.name() != null) {
            type.setName(request.name().trim());
        }
        if (request.defaultMaxScore() != null) {
            type.setDefaultMaxScore(request.defaultMaxScore());
        }
        if (request.active() != null) {
            type.setIsActive(request.active());
            // Keep legacy weight column in sync: active counts, inactive does not.
            type.setWeight(Boolean.TRUE.equals(request.active()) ? BigDecimal.valueOf(100) : BigDecimal.ZERO);
        }
        type = assessmentTypeLookupRepository.save(type);
        auditService.logAdminAction(actor, "UPDATE", "assessment_type", type.getId(), type.getCode());
        return AssessmentTypeResponse.from(type);
    }

    private String generateUniqueCode(String name) {
        String base = toCode(name);
        if (!assessmentTypeLookupRepository.existsByCode(base)) {
            return base;
        }
        for (int i = 2; i < 100; i++) {
            String candidate = base.length() > 17 ? base.substring(0, 17) + "_" + i : base + "_" + i;
            if (candidate.length() > 20) {
                candidate = candidate.substring(0, 20);
            }
            if (!assessmentTypeLookupRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ConflictException("Could not generate a unique assessment type code");
    }

    static String toCode(String name) {
        String cleaned = name == null ? "" : name.trim().toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        if (cleaned.isEmpty()) {
            cleaned = "TYPE";
        }
        if (!Character.isLetter(cleaned.charAt(0))) {
            cleaned = "T_" + cleaned;
        }
        if (cleaned.length() > 20) {
            cleaned = cleaned.substring(0, 20).replaceAll("_+$", "");
        }
        return cleaned;
    }

    private int nextSortOrder() {
        return assessmentTypeLookupRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
            .map(AssessmentTypeLookup::getSortOrder)
            .filter(order -> order != null)
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void requireAdmin(SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can manage assessment types");
        }
    }
}
