package com.example.backend.service;

import com.example.backend.dto.student.ImportCsvJobAcceptedResponse;
import com.example.backend.dto.student.ImportCsvJobResponse;
import com.example.backend.dto.student.ImportCsvRowResult;
import com.example.backend.entity.CsvImportJob;
import com.example.backend.entity.enums.CsvImportJobStatus;
import com.example.backend.entity.enums.CsvImportJobType;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.importhub.StudentCsvImportPublisher;
import com.example.backend.repository.CsvImportJobRepository;
import com.example.backend.security.CustomUserDetailsService;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.CsvParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentCsvImportService {

    private static final Logger log = LoggerFactory.getLogger(StudentCsvImportService.class);

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
    private final CsvImportJobRepository csvImportJobRepository;
    private final StudentCsvImportPublisher studentCsvImportPublisher;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    public StudentCsvImportService(
        ActiveSessionService activeSessionService,
        StudentCsvImportRowService rowService,
        CsvImportJobRepository csvImportJobRepository,
        StudentCsvImportPublisher studentCsvImportPublisher,
        CustomUserDetailsService customUserDetailsService,
        ObjectMapper objectMapper) {
        this.activeSessionService = activeSessionService;
        this.rowService = rowService;
        this.csvImportJobRepository = csvImportJobRepository;
        this.studentCsvImportPublisher = studentCsvImportPublisher;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
    }

    public ImportCsvJobAcceptedResponse enqueueImport(MultipartFile file, SecurityUser actor) {
        ValidatedCsv validated = validateAndRead(file);

        CsvImportJob job = new CsvImportJob();
        job.setType(CsvImportJobType.STUDENT);
        job.setStatus(CsvImportJobStatus.PENDING);
        job.setActorUserId(actor.getUserId());
        job.setOriginalFilename(file.getOriginalFilename());
        job.setFileBytes(validated.bytes());
        job.setTotalRows(validated.dataRowCount());
        job = csvImportJobRepository.save(job);

        boolean published = studentCsvImportPublisher.publish(job.getId());
        if (!published) {
            executeJob(job.getId());
            job = csvImportJobRepository.findById(job.getId()).orElse(job);
            return new ImportCsvJobAcceptedResponse(job.getId(), job.getStatus().name());
        }
        return new ImportCsvJobAcceptedResponse(job.getId(), CsvImportJobStatus.PENDING.name());
    }

    public ImportCsvJobResponse getJob(Long jobId) {
        CsvImportJob job = csvImportJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));
        if (job.getType() != CsvImportJobType.STUDENT) {
            throw new ResourceNotFoundException("Import job not found");
        }
        return toResponse(job);
    }

    public void executeJob(Long jobId) {
        CsvImportJob job = csvImportJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));

        if (job.getStatus() == CsvImportJobStatus.COMPLETED || job.getStatus() == CsvImportJobStatus.FAILED) {
            return;
        }

        job.setStatus(CsvImportJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        job.setErrorMessage(null);
        csvImportJobRepository.save(job);

        SecurityUser actor = customUserDetailsService.loadSecurityUserById(job.getActorUserId());

        List<String[]> parsed;
        try {
            parsed = CsvParser.parse(new ByteArrayInputStream(job.getFileBytes()));
        } catch (IOException ex) {
            markFailed(jobId, "Failed to read CSV file");
            return;
        }

        if (parsed.isEmpty()) {
            markFailed(jobId, "CSV file is empty");
            return;
        }

        List<String> headers = normalizeHeaders(parsed.get(0));
        try {
            validateHeaders(headers);
        } catch (BadRequestException ex) {
            markFailed(jobId, ex.getMessage());
            return;
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
                results.add(new ImportCsvRowResult(
                    rowNumber, "FAILED", "Missing required field: admission_number", null, null));
                failureCount++;
                continue;
            }
            if (!seenAdmissionNumbers.add(admissionNumber.toLowerCase(Locale.ROOT))) {
                results.add(new ImportCsvRowResult(
                    rowNumber,
                    "FAILED",
                    "Duplicate admission number in file: " + admissionNumber,
                    null,
                    null));
                failureCount++;
                continue;
            }

            ImportCsvRowResult result;
            try {
                result = rowService.processRow(rowNumber, row, actor);
            } catch (RuntimeException ex) {
                result = new ImportCsvRowResult(
                    rowNumber,
                    "FAILED",
                    ex.getMessage() == null ? "Import row failed" : ex.getMessage(),
                    null,
                    null);
            }
            results.add(result);
            if ("IMPORTED".equals(result.status())) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        job = csvImportJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));
        job.setStatus(CsvImportJobStatus.COMPLETED);
        job.setSuccessCount(successCount);
        job.setFailureCount(failureCount);
        job.setTotalRows(results.size());
        job.setResultsJson(writeResults(results));
        job.setCompletedAt(LocalDateTime.now());
        csvImportJobRepository.save(job);
        log.info(
            "Student CSV import completed: jobId={} success={} failure={}",
            jobId,
            successCount,
            failureCount);
    }

    @Transactional
    public void markFailed(Long jobId, String message) {
        csvImportJobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() == CsvImportJobStatus.COMPLETED) {
                return;
            }
            job.setStatus(CsvImportJobStatus.FAILED);
            job.setErrorMessage(message == null ? "Import failed" : message);
            job.setCompletedAt(LocalDateTime.now());
            csvImportJobRepository.save(job);
        });
    }

    private ValidatedCsv validateAndRead(MultipartFile file) {
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

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read CSV file");
        }

        List<String[]> parsed;
        try {
            parsed = CsvParser.parse(new ByteArrayInputStream(bytes));
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read CSV file");
        }

        if (parsed.isEmpty()) {
            throw new BadRequestException("CSV file is empty");
        }

        List<String> headers = normalizeHeaders(parsed.get(0));
        validateHeaders(headers);

        if (parsed.size() == 1) {
            throw new BadRequestException("CSV file has no data rows");
        }

        int dataRowCount = parsed.size() - 1;
        if (dataRowCount > MAX_ROWS) {
            throw new BadRequestException("CSV file exceeds maximum of " + MAX_ROWS + " data rows");
        }

        return new ValidatedCsv(bytes, dataRowCount);
    }

    private ImportCsvJobResponse toResponse(CsvImportJob job) {
        return new ImportCsvJobResponse(
            job.getId(),
            job.getStatus().name(),
            job.getTotalRows(),
            job.getSuccessCount(),
            job.getFailureCount(),
            readResults(job.getResultsJson()),
            job.getErrorMessage());
    }

    private String writeResults(List<ImportCsvRowResult> results) {
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize import results", ex);
        }
    }

    private List<ImportCsvRowResult> readResults(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize import results JSON", ex);
            return Collections.emptyList();
        }
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

    private record ValidatedCsv(byte[] bytes, int dataRowCount) {
    }
}
