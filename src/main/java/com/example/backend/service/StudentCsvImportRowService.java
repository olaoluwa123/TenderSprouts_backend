package com.example.backend.service;

import com.example.backend.dto.parent.OnboardParentForStudentRequest;
import com.example.backend.dto.student.CreateStudentRequest;
import com.example.backend.dto.student.ImportCsvRowResult;
import com.example.backend.dto.student.StudentResponse;
import com.example.backend.entity.enums.Gender;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentCsvImportRowService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final StudentService studentService;
    private final ParentService parentService;
    private final SchoolClassRepository schoolClassRepository;

    public StudentCsvImportRowService(
        StudentService studentService,
        ParentService parentService,
        SchoolClassRepository schoolClassRepository) {
        this.studentService = studentService;
        this.parentService = parentService;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportCsvRowResult processRow(
        int rowNumber,
        Map<String, String> row,
        SecurityUser actor) {
        try {
            String firstName = require(row, "student_first_name");
            String lastName = require(row, "student_last_name");
            String admissionNumber = require(row, "admission_number");
            String className = require(row, "class");
            String parentEmail = require(row, "parent_email");
            String parentFullName = require(row, "parent_full_name");

            if (!EMAIL_PATTERN.matcher(parentEmail).matches()) {
                throw new IllegalArgumentException("Invalid parent email: " + parentEmail);
            }

            Long classId = schoolClassRepository.findFirstByNameIgnoreCase(className.trim())
                .map(c -> c.getId())
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + className));

            Gender gender = parseGender(row.get("gender"));
            LocalDate dateOfBirth = parseDate(row.get("date_of_birth"));
            String parentPhone = blankToNull(row.get("parent_phone"));

            StudentResponse student = studentService.create(
                new CreateStudentRequest(
                    firstName,
                    lastName,
                    dateOfBirth,
                    admissionNumber,
                    gender,
                    classId,
                    blankToNull(row.get("height")),
                    blankToNull(row.get("weight"))),
                actor);

            ParentService.ParentOnboardResult parentResult = parentService.onboardOrLinkForStudent(
                student.id(),
                new OnboardParentForStudentRequest(parentEmail, parentFullName, parentPhone),
                actor);

            String message = parentResult.linkedExisting()
                ? "Imported (linked existing parent)"
                : "Imported";
            return new ImportCsvRowResult(
                rowNumber,
                "IMPORTED",
                message,
                student.id(),
                parentResult.parent().id());
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

    private static Gender parseGender(String value) {
        if (value == null || value.isBlank()) {
            return Gender.MALE;
        }
        try {
            return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid gender: " + value);
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date_of_birth (use YYYY-MM-DD): " + value);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ImportCsvRowResult failed(int rowNumber, String message) {
        return new ImportCsvRowResult(rowNumber, "FAILED", message, null, null);
    }
}
