package com.example.backend.service;

import com.example.backend.dto.teacher.OnboardTeacherRequest;
import com.example.backend.dto.teacher.TeacherImportCsvRowResult;
import com.example.backend.dto.teacher.TeacherResponse;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.SecurityUser;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherCsvImportRowService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final TeacherService teacherService;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;

    public TeacherCsvImportRowService(
        TeacherService teacherService,
        UserRepository userRepository,
        SchoolClassRepository schoolClassRepository) {
        this.teacherService = teacherService;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TeacherImportCsvRowResult processRow(
        int rowNumber,
        Map<String, String> row,
        SecurityUser actor) {
        try {
            String email = require(row, "email");
            String fullName = require(row, "full_name");
            String phone = blankToNull(row.get("phone"));
            String className = blankToNull(row.get("class"));

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Invalid email: " + email);
            }

            if (userRepository.findByEmail(email).isPresent()) {
                return failed(rowNumber, "Email already registered");
            }

            Long classId = null;
            if (className != null) {
                classId = schoolClassRepository.findFirstByNameIgnoreCase(className)
                    .map(c -> c.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Class not found: " + className));
            }

            TeacherResponse teacher = teacherService.onboard(
                new OnboardTeacherRequest(email, fullName, phone, classId),
                actor);

            String message = classId != null
                ? "Imported - assigned to class, welcome email sent"
                : "Imported - welcome email sent with temporary password";

            return new TeacherImportCsvRowResult(
                rowNumber,
                "IMPORTED",
                message,
                teacher.id());
        } catch (ApiException ex) {
            return failed(rowNumber, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return failed(rowNumber, ex.getMessage());
        }
    }

    static Map<String, String> toRowMap(List<String> headers, String[] values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = headers.get(i).trim().toLowerCase(Locale.ROOT);
            String value = i < values.length ? values[i] : "";
            row.put(key, value != null ? value.trim() : "");
        }
        return row;
    }

    private static String require(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static TeacherImportCsvRowResult failed(int rowNumber, String message) {
        return new TeacherImportCsvRowResult(rowNumber, "FAILED", message, null);
    }
}
