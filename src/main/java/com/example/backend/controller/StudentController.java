package com.example.backend.controller;

import com.example.backend.dto.parent.OnboardParentForStudentRequest;
import com.example.backend.dto.parent.ParentResponse;
import com.example.backend.dto.student.CreateEnrollmentRequest;
import com.example.backend.dto.student.CreateStudentRequest;
import com.example.backend.dto.student.EnrollmentResponse;
import com.example.backend.dto.student.ImportCsvJobAcceptedResponse;
import com.example.backend.dto.student.ImportCsvJobResponse;
import com.example.backend.dto.student.OnboardStudentWithParentRequest;
import com.example.backend.dto.student.OnboardStudentWithParentResponse;
import com.example.backend.dto.student.StudentProfileResponse;
import com.example.backend.dto.student.StudentResponse;
import com.example.backend.dto.student.UpdateStudentRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.ParentService;
import com.example.backend.dto.grade.GradeResponse;
import com.example.backend.dto.grade.GradingSheetResponse;
import com.example.backend.dto.grade.SaveStudentGradesRequest;
import com.example.backend.service.AuthorizationService;
import com.example.backend.service.GradeService;
import com.example.backend.service.StudentCsvImportService;
import com.example.backend.service.StudentProfileService;
import com.example.backend.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentProfileService studentProfileService;
    private final ParentService parentService;
    private final GradeService gradeService;
    private final AuthorizationService authorizationService;
    private final StudentCsvImportService studentCsvImportService;

    public StudentController(
        StudentService studentService,
        StudentProfileService studentProfileService,
        ParentService parentService,
        GradeService gradeService,
        AuthorizationService authorizationService,
        StudentCsvImportService studentCsvImportService) {
        this.studentService = studentService;
        this.studentProfileService = studentProfileService;
        this.parentService = parentService;
        this.gradeService = gradeService;
        this.authorizationService = authorizationService;
        this.studentCsvImportService = studentCsvImportService;
    }

    @GetMapping
    public Page<StudentResponse> list(
        @RequestParam(required = false) Long classId,
        @RequestParam(required = false) Long sessionId,
        @RequestParam(required = false) String search,
        Pageable pageable) {
        return studentService.list(classId, sessionId, search, pageable, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id) {
        authorizationService.requireParentOrStaffForStudentRead(SecurityUtils.requireCurrentUser(), id);
        return studentService.getById(id);
    }

    @GetMapping("/{id}/profile")
    public StudentProfileResponse getProfile(@PathVariable Long id) {
        authorizationService.requireParentOrStaffForStudentRead(SecurityUtils.requireCurrentUser(), id);
        return studentProfileService.getProfile(id, SecurityUtils.requireCurrentUser());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/onboard")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardStudentWithParentResponse onboardWithParent(
        @Valid @RequestBody OnboardStudentWithParentRequest request) {
        return studentService.onboardWithParent(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportCsvJobAcceptedResponse importCsv(@RequestParam("file") MultipartFile file) {
        return studentCsvImportService.enqueueImport(file, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/import-csv/jobs/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ImportCsvJobResponse getImportCsvJob(@PathVariable Long jobId) {
        return studentCsvImportService.getJob(jobId);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(id, request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/onboard-parent")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ParentResponse onboardParent(
        @PathVariable Long id,
        @Valid @RequestBody OnboardParentForStudentRequest request) {
        return parentService.onboardForStudent(id, request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/enrollments")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(
        @PathVariable Long id,
        @Valid @RequestBody CreateEnrollmentRequest request) {
        return studentService.enroll(id, request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{id}/enrollments")
    public List<EnrollmentResponse> listEnrollments(@PathVariable Long id) {
        authorizationService.requireParentOrStaffForStudentRead(SecurityUtils.requireCurrentUser(), id);
        return studentService.listEnrollments(id);
    }

    @GetMapping("/{id}/grading-sheet")
    public GradingSheetResponse gradingSheet(
        @PathVariable Long id,
        @RequestParam Long termId) {
        return gradeService.getGradingSheet(id, termId, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/{id}/grades")
    public GradingSheetResponse saveGrades(
        @PathVariable Long id,
        @Valid @RequestBody SaveStudentGradesRequest request) {
        return gradeService.upsertStudentGrades(id, request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{id}/grades")
    public List<GradeResponse> listGrades(
        @PathVariable Long id,
        @RequestParam(required = false) Long termId) {
        return gradeService.listForStudent(id, termId, SecurityUtils.requireCurrentUser());
    }
}
