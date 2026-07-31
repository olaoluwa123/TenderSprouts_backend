package com.example.backend.service;

import com.example.backend.dto.student.ImportCsvResponse;
import com.example.backend.dto.student.ImportCsvRowResult;
import com.example.backend.exception.BadRequestException;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.CsvParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentCsvImportService {

    private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ROWS = 500;
    private static final List<String> REQUIRED_HEADERS = List.of(
        "student_first_name",
        "student_last_name",
        "admission_number",
        "class",
        "parent_email",
        "parent_full_name");

    private final ActiveSessionService activeSessionService;
    private final StudentCsvImportRowService rowService;

    public StudentCsvImportService(
        ActiveSessionService activeSessionService,
        StudentCsvImportRowService rowService) {
        this.activeSessionService = activeSessionService;
        this.rowService = rowService;
    }

    public ImportCsvResponse importFile(MultipartFile file, SecurityUser actor) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BadRequestException("Only .csv files are supported");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("CSV file exceeds maximum size of 2 MB");
        }

        activeSessionService.requireActiveSessionId();

        List<String[]> parsed;
        try {
            parsed = CsvParser.parse(file.getInputStream());
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read CSV file");
        }

        if (parsed.isEmpty()) {
            throw new BadRequestException("CSV file is empty");
        }

        String[] headerRow = parsed.get(0);
        List<String> headers = normalizeHeaders(headerRow);
        validateHeaders(headers);

        if (parsed.size() == 1) {
            throw new BadRequestException("CSV file has no data rows");
        }

        int dataRowCount = parsed.size() - 1;
        if (dataRowCount > MAX_ROWS) {
            throw new BadRequestException("CSV file exceeds maximum of " + MAX_ROWS + " data rows");
        }

        List<ImportCsvRowResult> results = new ArrayList<>();
        Set<String> seenAdmissionNumbers = new HashSet<>();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 1; i < parsed.size(); i++) {
            int rowNumber = i + 1;
            Map<String, String> row = StudentCsvImportRowService.toRowMap(headers, parsed.get(i));
            String admissionNumber = row.getOrDefault("admission_number", "").trim();
            if (admissionNumber.isBlank()) {
                ImportCsvRowResult result = new ImportCsvRowResult(
                    rowNumber, "FAILED", "Missing required field: admission_number", null, null);
                results.add(result);
                failureCount++;
                continue;
            }
            if (!seenAdmissionNumbers.add(admissionNumber.toLowerCase(Locale.ROOT))) {
                ImportCsvRowResult result = new ImportCsvRowResult(
                    rowNumber, "FAILED", "Duplicate admission number in file: " + admissionNumber, null, null);
                results.add(result);
                failureCount++;
                continue;
            }

            ImportCsvRowResult result = rowService.processRow(rowNumber, row, actor);
            results.add(result);
            if ("IMPORTED".equals(result.status())) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        return new ImportCsvResponse(dataRowCount, successCount, failureCount, results);
    }

    private static List<String> normalizeHeaders(String[] headerRow) {
        List<String> headers = new ArrayList<>();
        for (String header : headerRow) {
            String normalized = header == null ? "" : header.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("\uFEFF")) {
                normalized = normalized.substring(1);
            }
            headers.add(normalized);
        }
        return headers;
    }

    private static void validateHeaders(List<String> headers) {
        for (String required : REQUIRED_HEADERS) {
            if (!headers.contains(required)) {
                throw new BadRequestException("Missing required CSV column: " + required);
            }
        }
    }
}
