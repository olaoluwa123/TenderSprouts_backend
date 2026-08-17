package com.example.backend.service;

import com.example.backend.dto.teacher.TeacherImportCsvResponse;
import com.example.backend.dto.teacher.TeacherImportCsvRowResult;
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
public class TeacherCsvImportService {

    private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ROWS = 500;
    private static final List<String> REQUIRED_HEADERS = List.of("email", "full_name");

    private final TeacherCsvImportRowService rowService;

    public TeacherCsvImportService(TeacherCsvImportRowService rowService) {
        this.rowService = rowService;
    }

    public TeacherImportCsvResponse importFile(MultipartFile file, SecurityUser actor) {
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

        List<TeacherImportCsvRowResult> results = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 1; i < parsed.size(); i++) {
            int rowNumber = i + 1;
            Map<String, String> row = TeacherCsvImportRowService.toRowMap(headers, parsed.get(i));
            String email = row.getOrDefault("email", "").trim();
            if (email.isBlank()) {
                results.add(new TeacherImportCsvRowResult(
                    rowNumber, "FAILED", "Missing required field: email", null));
                failureCount++;
                continue;
            }
            if (!seenEmails.add(email.toLowerCase(Locale.ROOT))) {
                results.add(new TeacherImportCsvRowResult(
                    rowNumber, "FAILED", "Duplicate email in file: " + email, null));
                failureCount++;
                continue;
            }

            TeacherImportCsvRowResult result = rowService.processRow(rowNumber, row, actor);
            results.add(result);
            if ("IMPORTED".equals(result.status())) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        return new TeacherImportCsvResponse(dataRowCount, successCount, failureCount, results);
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
