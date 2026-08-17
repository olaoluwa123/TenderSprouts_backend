package com.example.backend.service;

import com.example.backend.dto.fee.BatchFeeAmountItem;
import com.example.backend.dto.fee.BatchFeeAmountsRequest;
import com.example.backend.dto.fee.CreateFeeStructureRequest;
import com.example.backend.dto.fee.FeeStructureResponse;
import com.example.backend.dto.fee.FeeTemplateItemRequest;
import com.example.backend.dto.fee.FeeTemplateItemResponse;
import com.example.backend.dto.fee.FeeTemplateResponse;
import com.example.backend.dto.fee.GeneratePupilFeesResponse;
import com.example.backend.dto.fee.InvoiceResponse;
import com.example.backend.dto.fee.ParentChildFeesResponse;
import com.example.backend.dto.fee.ParentFeesResponse;
import com.example.backend.dto.fee.PublishFeesResponse;
import com.example.backend.dto.fee.RepublishFeesPreviewResponse;
import com.example.backend.dto.fee.RepublishFeesResponse;
import com.example.backend.dto.fee.RepublishPupilPreviewItem;
import com.example.backend.dto.fee.SaveFeeTemplateRequest;
import com.example.backend.dto.fee.SchoolFeeTemplateResponse;
import com.example.backend.dto.fee.UpdateFeeStructureRequest;
import com.example.backend.entity.FeeSchoolTemplateItem;
import com.example.backend.entity.FeeStructure;
import com.example.backend.entity.FeeTemplate;
import com.example.backend.entity.FeeTemplateItem;
import com.example.backend.entity.Invoice;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.InvoiceStatus;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.FeeSchoolTemplateItemRepository;
import com.example.backend.repository.FeeStructureRepository;
import com.example.backend.repository.FeeTemplateRepository;
import com.example.backend.repository.InvoiceRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.security.SecurityUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeService {

    private final FeeStructureRepository feeStructureRepository;
    private final FeeTemplateRepository feeTemplateRepository;
    private final FeeSchoolTemplateItemRepository feeSchoolTemplateItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TermRepository termRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final AuditService auditService;
    private final ActiveTermService activeTermService;

    public FeeService(
        FeeStructureRepository feeStructureRepository,
        FeeTemplateRepository feeTemplateRepository,
        FeeSchoolTemplateItemRepository feeSchoolTemplateItemRepository,
        InvoiceRepository invoiceRepository,
        SchoolClassRepository schoolClassRepository,
        TermRepository termRepository,
        StudentRepository studentRepository,
        ParentRepository parentRepository,
        ParentStudentRepository parentStudentRepository,
        AuditService auditService,
        ActiveTermService activeTermService) {
        this.feeStructureRepository = feeStructureRepository;
        this.feeTemplateRepository = feeTemplateRepository;
        this.feeSchoolTemplateItemRepository = feeSchoolTemplateItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.termRepository = termRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.auditService = auditService;
        this.activeTermService = activeTermService;
    }

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> listStructures(Long classId, Long termId) {
        if (classId == null || termId == null) {
            throw new ConflictException("classId and termId are required");
        }
        return feeStructureRepository.findByClassIdAndTermIdOrderByNameAsc(classId, termId).stream()
            .map(this::toStructureResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public FeeTemplateResponse getTemplate(Long classId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        return feeTemplateRepository.findByClassId(classId)
            .map(template -> toTemplateResponse(template, schoolClass.getName()))
            .orElseGet(() -> new FeeTemplateResponse(null, classId, schoolClass.getName(), List.of()));
    }

    @Transactional(readOnly = true)
    public SchoolFeeTemplateResponse getSchoolTemplate() {
        return new SchoolFeeTemplateResponse(toSchoolItemResponses(loadSchoolItems()), 0);
    }

    @Transactional
    public SchoolFeeTemplateResponse saveSchoolTemplate(SaveFeeTemplateRequest request, SecurityUser actor) {
        List<FeeTemplateItemRequest> items = validateTemplateItems(request.items());

        feeSchoolTemplateItemRepository.deleteAll();
        feeSchoolTemplateItemRepository.flush();
        int index = 0;
        for (FeeTemplateItemRequest itemRequest : items) {
            FeeSchoolTemplateItem item = new FeeSchoolTemplateItem();
            item.setName(itemRequest.name().trim());
            item.setDefaultAmount(itemRequest.defaultAmount());
            item.setDescription(trimToNull(itemRequest.description()));
            item.setSortOrder(itemRequest.sortOrder() != null ? itemRequest.sortOrder() : index);
            item.setIsActive(itemRequest.isActive() == null || Boolean.TRUE.equals(itemRequest.isActive()));
            feeSchoolTemplateItemRepository.save(item);
            index++;
        }

        int synced = 0;
        for (SchoolClass schoolClass : schoolClassRepository.findAll()) {
            if (Boolean.FALSE.equals(schoolClass.getIsActive())) {
                continue;
            }
            replaceClassTemplate(schoolClass.getId(), items);
            synced++;
        }

        auditService.logAdminAction(actor, "UPSERT", "fee_school_template", 0L, "classesSynced=" + synced);
        return new SchoolFeeTemplateResponse(toSchoolItemResponses(loadSchoolItems()), synced);
    }

    @Transactional
    public FeeTemplateResponse saveTemplate(Long classId, SaveFeeTemplateRequest request, SecurityUser actor) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        List<FeeTemplateItemRequest> items = validateTemplateItems(request.items());
        FeeTemplate saved = replaceClassTemplate(classId, items);
        auditService.logAdminAction(actor, "UPSERT", "fee_template", saved.getId(), "classId=" + classId);
        return toTemplateResponse(saved, schoolClass.getName());
    }

    @Transactional
    public List<FeeStructureResponse> ensureFromTemplate(Long classId, Long termId, SecurityUser actor) {
        if (classId == null || termId == null) {
            throw new ConflictException("classId and termId are required");
        }
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        Term term = termRepository.findById(termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));

        FeeTemplate template = feeTemplateRepository.findByClassId(classId).orElse(null);
        if (template == null || template.getItems().isEmpty()) {
            List<FeeSchoolTemplateItem> schoolItems = loadSchoolItems().stream()
                .filter(item -> !Boolean.FALSE.equals(item.getIsActive()))
                .toList();
            if (!schoolItems.isEmpty()) {
                List<FeeTemplateItemRequest> requests = schoolItems.stream()
                    .map(item -> new FeeTemplateItemRequest(
                        item.getName(),
                        item.getDefaultAmount(),
                        item.getDescription(),
                        item.getSortOrder(),
                        item.getIsActive()))
                    .toList();
                template = replaceClassTemplate(classId, requests);
            }
        }

        if (template != null) {
            List<FeeTemplateItem> activeItems = template.getItems().stream()
                .filter(item -> !Boolean.FALSE.equals(item.getIsActive()))
                .toList();
            int created = 0;
            for (FeeTemplateItem item : activeItems) {
                var existing = feeStructureRepository.findByClassIdAndTermIdAndNameIgnoreCase(
                    classId, termId, item.getName());
                if (existing.isPresent()) {
                    continue;
                }
                FeeStructure fee = new FeeStructure();
                fee.setName(item.getName());
                fee.setClassId(schoolClass.getId());
                fee.setTermId(term.getId());
                fee.setSessionId(term.getSessionId());
                fee.setAmount(item.getDefaultAmount());
                fee.setDescription(item.getDescription());
                fee.setIsActive(true);
                feeStructureRepository.save(fee);
                created++;
            }
            if (created > 0) {
                auditService.logAdminAction(
                    actor,
                    "ENSURE_FROM_TEMPLATE",
                    "fee_structure",
                    classId,
                    "termId=" + termId + ", created=" + created);
            }
        }
        return listStructures(classId, termId);
    }

    private List<FeeTemplateItemRequest> validateTemplateItems(List<FeeTemplateItemRequest> rawItems) {
        List<FeeTemplateItemRequest> items = rawItems == null ? List.of() : rawItems;
        Set<String> seenNames = new HashSet<>();
        for (FeeTemplateItemRequest item : items) {
            String key = item.name().trim().toLowerCase(Locale.ROOT);
            if (!seenNames.add(key)) {
                throw new ConflictException("Duplicate fee item name in template: " + item.name().trim());
            }
        }
        return items;
    }

    private FeeTemplate replaceClassTemplate(Long classId, List<FeeTemplateItemRequest> items) {
        FeeTemplate template = feeTemplateRepository.findByClassId(classId).orElseGet(() -> {
            FeeTemplate created = new FeeTemplate();
            created.setClassId(classId);
            return created;
        });
        template.clearItems();
        // Flush orphan deletes before re-inserting same names (avoids unique key conflicts).
        feeTemplateRepository.saveAndFlush(template);
        int index = 0;
        for (FeeTemplateItemRequest itemRequest : items) {
            FeeTemplateItem item = new FeeTemplateItem();
            item.setName(itemRequest.name().trim());
            item.setDefaultAmount(itemRequest.defaultAmount());
            item.setDescription(trimToNull(itemRequest.description()));
            item.setSortOrder(itemRequest.sortOrder() != null ? itemRequest.sortOrder() : index);
            item.setIsActive(itemRequest.isActive() == null || Boolean.TRUE.equals(itemRequest.isActive()));
            template.addItem(item);
            index++;
        }
        return feeTemplateRepository.save(template);
    }

    private List<FeeSchoolTemplateItem> loadSchoolItems() {
        return feeSchoolTemplateItemRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    private List<FeeTemplateItemResponse> toSchoolItemResponses(List<FeeSchoolTemplateItem> items) {
        return items.stream()
            .map(item -> new FeeTemplateItemResponse(
                item.getId(),
                item.getName(),
                item.getDefaultAmount(),
                item.getDescription(),
                item.getSortOrder(),
                Boolean.TRUE.equals(item.getIsActive())))
            .toList();
    }

    @Transactional
    public List<FeeStructureResponse> batchUpdateAmounts(BatchFeeAmountsRequest request, SecurityUser actor) {
        List<FeeStructureResponse> updated = new ArrayList<>();
        for (BatchFeeAmountItem item : request.items()) {
            FeeStructure fee = findStructure(item.id());
            if (fee.getPublishedAt() != null
                && item.amount().compareTo(fee.getAmount()) != 0) {
                throw new ConflictException(
                    "Cannot change amount after fees are published: " + fee.getName());
            }
            fee.setAmount(item.amount());
            if (item.dueDate() != null) {
                fee.setDueDate(item.dueDate());
            }
            FeeStructure saved = feeStructureRepository.save(fee);
            updated.add(toStructureResponse(saved));
        }
        auditService.logAdminAction(actor, "BATCH_AMOUNTS", "fee_structure", 0L,
            "count=" + updated.size());
        return updated;
    }

    @Transactional
    public FeeStructureResponse createStructure(CreateFeeStructureRequest request, SecurityUser actor) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        Term term = termRepository.findById(request.termId())
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));

        String name = request.name().trim();
        feeStructureRepository.findByClassIdAndTermIdAndNameIgnoreCase(request.classId(), request.termId(), name)
            .ifPresent(existing -> {
                throw new ConflictException("A fee line item with this name already exists for the class/term");
            });

        FeeStructure fee = new FeeStructure();
        fee.setName(name);
        fee.setClassId(schoolClass.getId());
        fee.setTermId(term.getId());
        fee.setSessionId(term.getSessionId());
        fee.setAmount(request.amount());
        fee.setDescription(trimToNull(request.description()));
        fee.setDueDate(request.dueDate());
        fee.setIsActive(true);

        FeeStructure saved = feeStructureRepository.save(fee);
        auditService.logAdminAction(actor, "CREATE", "fee_structure", saved.getId(), saved.getName());
        return toStructureResponse(saved);
    }

    @Transactional
    public FeeStructureResponse updateStructure(Long id, UpdateFeeStructureRequest request, SecurityUser actor) {
        FeeStructure fee = findStructure(id);
        if (fee.getPublishedAt() != null
            && request.amount() != null
            && request.amount().compareTo(fee.getAmount()) != 0) {
            throw new ConflictException("Cannot change amount after fees are published to parents");
        }
        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            feeStructureRepository.findByClassIdAndTermIdAndNameIgnoreCase(fee.getClassId(), fee.getTermId(), name)
                .filter(other -> !Objects.equals(other.getId(), fee.getId()))
                .ifPresent(other -> {
                    throw new ConflictException("A fee line item with this name already exists for the class/term");
                });
            fee.setName(name);
        }
        if (request.amount() != null) {
            fee.setAmount(request.amount());
        }
        if (request.description() != null) {
            fee.setDescription(trimToNull(request.description()));
        }
        if (request.dueDate() != null) {
            fee.setDueDate(request.dueDate());
        }
        if (request.isActive() != null) {
            fee.setIsActive(request.isActive());
        }
        FeeStructure saved = feeStructureRepository.save(fee);
        auditService.logAdminAction(actor, "UPDATE", "fee_structure", saved.getId(), saved.getName());
        return toStructureResponse(saved);
    }

    @Transactional
    public void deleteStructure(Long id, SecurityUser actor) {
        FeeStructure fee = findStructure(id);
        if (fee.getPublishedAt() != null || invoiceRepository.countByFeeStructureId(id) > 0) {
            fee.setIsActive(false);
            feeStructureRepository.save(fee);
            auditService.logAdminAction(actor, "DEACTIVATE", "fee_structure", id, fee.getName());
            return;
        }
        feeStructureRepository.delete(fee);
        auditService.logAdminAction(actor, "DELETE", "fee_structure", id, fee.getName());
    }

    @Transactional
    public PublishFeesResponse publishClassTerm(Long classId, Long termId, SecurityUser actor) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }
        if (!termRepository.existsById(termId)) {
            throw new ResourceNotFoundException("Term not found");
        }
        List<FeeStructure> structures = feeStructureRepository
            .findByClassIdAndTermIdAndIsActiveTrueOrderByNameAsc(classId, termId);
        if (structures.isEmpty()) {
            throw new ConflictException("No active fee line items to publish for this class/term");
        }

        List<Student> pupils = studentRepository.findByClassId(classId).stream()
            .filter(s -> !Boolean.FALSE.equals(s.getIsActive()))
            .toList();
        if (pupils.isEmpty()) {
            throw new ConflictException("No active pupils in this class to invoice");
        }

        int structuresPublished = 0;
        int invoicesCreated = 0;
        LocalDateTime now = LocalDateTime.now();

        for (FeeStructure fee : structures) {
            boolean newlyPublished = fee.getPublishedAt() == null;
            if (newlyPublished) {
                fee.setPublishedAt(now);
                feeStructureRepository.save(fee);
                structuresPublished++;
            }
            for (Student pupil : pupils) {
                if (invoiceRepository.findByStudentIdAndFeeStructureId(pupil.getId(), fee.getId()).isPresent()) {
                    continue;
                }
                Invoice invoice = new Invoice();
                invoice.setStudentId(pupil.getId());
                invoice.setFeeStructureId(fee.getId());
                invoice.setAmount(fee.getAmount());
                invoice.setDueDate(fee.getDueDate());
                invoice.setStatus(InvoiceStatus.PENDING);
                invoiceRepository.save(invoice);
                invoicesCreated++;
            }
        }

        auditService.logAdminAction(
            actor,
            "PUBLISH",
            "fee_structure",
            classId,
            "termId=" + termId + ", invoices=" + invoicesCreated);
        return new PublishFeesResponse(classId, termId, structuresPublished, invoicesCreated);
    }

    @Transactional(readOnly = true)
    public RepublishFeesPreviewResponse previewRepublishForNewPupils(Long classId, Long termId) {
        validateClassAndTerm(classId, termId);
        List<FeeStructure> publishedStructures = publishedActiveStructures(classId, termId);
        List<Student> pupils = activePupilsInClass(classId);
        List<RepublishPupilPreviewItem> missing = pupils.stream()
            .map(pupil -> {
                int missingCount = countMissingInvoices(pupil.getId(), publishedStructures);
                if (missingCount == 0) {
                    return null;
                }
                return new RepublishPupilPreviewItem(
                    pupil.getId(),
                    formatStudentName(pupil),
                    pupil.getAdmissionNumber(),
                    missingCount);
            })
            .filter(Objects::nonNull)
            .toList();
        return new RepublishFeesPreviewResponse(
            classId,
            termId,
            missing.size(),
            publishedStructures.size(),
            missing);
    }

    @Transactional
    public RepublishFeesResponse republishForNewPupils(Long classId, Long termId, SecurityUser actor) {
        validateClassAndTerm(classId, termId);
        List<FeeStructure> publishedStructures = publishedActiveStructures(classId, termId);
        if (publishedStructures.isEmpty()) {
            throw new ConflictException("Fees have not been published for this class/term yet");
        }

        List<Student> pupils = activePupilsInClass(classId);
        int pupilsInvoiced = 0;
        int invoicesCreated = 0;

        for (Student pupil : pupils) {
            int createdForPupil = createMissingInvoicesForStudent(pupil.getId(), publishedStructures);
            invoicesCreated += createdForPupil;
            if (createdForPupil > 0) {
                pupilsInvoiced++;
            }
        }

        auditService.logAdminAction(
            actor,
            "REPUBLISH",
            "fee_structure",
            classId,
            "termId=" + termId + ", pupils=" + pupilsInvoiced + ", invoices=" + invoicesCreated);
        return new RepublishFeesResponse(classId, termId, pupilsInvoiced, invoicesCreated);
    }

    @Transactional
    public GeneratePupilFeesResponse generateFeesForStudent(Long studentId, SecurityUser actor) {
        if (actor.getRole() != User.Role.PARENT || actor.getProfileId() == null) {
            throw new ForbiddenException("Parent profile required");
        }
        Parent parent = parentRepository.findById(actor.getProfileId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        if (!parentStudentRepository.existsByParentIdAndStudentId(parent.getId(), studentId)) {
            throw new ForbiddenException("Student is not linked to this parent");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (Boolean.FALSE.equals(student.getIsActive())) {
            throw new ConflictException("Cannot generate fees for an inactive pupil");
        }
        if (student.getClassId() == null) {
            throw new ConflictException("Pupil is not assigned to a class");
        }

        Term activeTerm = activeTermService.requireActiveTerm();
        Long classId = student.getClassId();
        Long termId = activeTerm.getId();
        List<FeeStructure> publishedStructures = publishedActiveStructures(classId, termId);
        if (publishedStructures.isEmpty()) {
            throw new ConflictException("No published fees for this pupil's class this term");
        }

        int invoicesCreated = createMissingInvoicesForStudent(student.getId(), publishedStructures);

        auditService.logAdminAction(
            actor,
            "GENERATE",
            "invoice",
            studentId,
            "classId=" + classId + ", termId=" + termId + ", invoices=" + invoicesCreated);
        return new GeneratePupilFeesResponse(studentId, classId, termId, invoicesCreated);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoicesForClassTerm(Long classId, Long termId) {
        List<FeeStructure> structures = feeStructureRepository.findByClassIdAndTermIdOrderByNameAsc(classId, termId);
        if (structures.isEmpty()) {
            return List.of();
        }
        Map<Long, FeeStructure> byId = structures.stream()
            .collect(Collectors.toMap(FeeStructure::getId, f -> f));
        List<Long> studentIds = studentRepository.findByClassId(classId).stream()
            .map(Student::getId)
            .toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Student> students = studentRepository.findAllById(studentIds).stream()
            .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, String> classNames = classNameMap();
        Map<Long, String> termNames = termNameMap();

        return invoiceRepository.findByStudentIdInOrderByDueDateAscCreatedAtDesc(studentIds).stream()
            .filter(inv -> byId.containsKey(inv.getFeeStructureId()))
            .map(inv -> {
                FeeStructure fee = byId.get(inv.getFeeStructureId());
                Student student = students.get(inv.getStudentId());
                return InvoiceResponse.from(
                    inv,
                    student == null ? null : formatStudentName(student),
                    student == null ? null : student.getAdmissionNumber(),
                    fee.getName(),
                    fee.getClassId(),
                    classNames.get(fee.getClassId()),
                    fee.getTermId(),
                    termNames.get(fee.getTermId()));
            })
            .toList();
    }

    @Transactional
    public InvoiceResponse markInvoicePaid(Long invoiceId, SecurityUser actor) {
        InvoiceResponse response = markInvoicePaidInternal(invoiceId);
        auditService.logAdminAction(actor, "MARK_PAID", "invoice", invoiceId, null);
        return response;
    }

    @Transactional
    public InvoiceResponse markInvoicePaidFromGateway(Long invoiceId, String gatewayReference) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
        }
        return toInvoiceResponse(invoice);
    }

    @Transactional
    public void markInvoicesPaidFromGateway(List<Long> invoiceIds, String gatewayReference) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return;
        }
        for (Long invoiceId : invoiceIds) {
            markInvoicePaidFromGateway(invoiceId, gatewayReference);
        }
    }

    @Transactional(readOnly = true)
    public List<Invoice> pendingPayableInvoicesForParent(SecurityUser actor) {
        if (actor.getRole() != User.Role.PARENT || actor.getProfileId() == null) {
            throw new ForbiddenException("Parent profile required");
        }
        Parent parent = parentRepository.findById(actor.getProfileId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        List<Long> childIds = parentStudentRepository.findByParentId(parent.getId()).stream()
            .map(ParentStudent::getStudentId)
            .toList();
        if (childIds.isEmpty()) {
            return List.of();
        }

        List<Invoice> invoices = invoiceRepository.findByStudentIdInAndStatus(childIds, InvoiceStatus.PENDING);
        if (invoices.isEmpty()) {
            return List.of();
        }

        Set<Long> feeIds = invoices.stream().map(Invoice::getFeeStructureId).collect(Collectors.toSet());
        Map<Long, FeeStructure> feesById = feeStructureRepository.findAllById(feeIds).stream()
            .collect(Collectors.toMap(FeeStructure::getId, fee -> fee));

        return invoices.stream()
            .filter(invoice -> {
                FeeStructure fee = feesById.get(invoice.getFeeStructureId());
                return fee != null
                    && fee.getPublishedAt() != null
                    && Boolean.TRUE.equals(fee.getIsActive())
                    && invoice.getAmount() != null
                    && invoice.getAmount().compareTo(BigDecimal.ZERO) > 0;
            })
            .toList();
    }

    public InvoiceResponse toInvoiceResponsePublic(Invoice invoice) {
        return toInvoiceResponse(invoice);
    }

    private InvoiceResponse markInvoicePaidInternal(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        invoice.setStatus(InvoiceStatus.PAID);
        Invoice saved = invoiceRepository.save(invoice);
        return toInvoiceResponse(saved);
    }

    @Transactional(readOnly = true)
    public ParentFeesResponse parentFees(SecurityUser actor) {
        if (actor.getRole() != User.Role.PARENT || actor.getProfileId() == null) {
            throw new ForbiddenException("Parent profile required");
        }
        Parent parent = parentRepository.findById(actor.getProfileId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        List<Long> childIds = parentStudentRepository.findByParentId(parent.getId()).stream()
            .map(ParentStudent::getStudentId)
            .toList();
        if (childIds.isEmpty()) {
            return new ParentFeesResponse(BigDecimal.ZERO, List.of());
        }

        Map<Long, Student> students = studentRepository.findAllById(childIds).stream()
            .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, String> classNames = classNameMap();
        Map<Long, String> termNames = termNameMap();
        List<Invoice> invoices = invoiceRepository.findByStudentIdInOrderByDueDateAscCreatedAtDesc(childIds);
        Map<Long, FeeStructure> feesById = new HashMap<>();
        Set<Long> feeIds = invoices.stream().map(Invoice::getFeeStructureId).collect(Collectors.toSet());
        if (!feeIds.isEmpty()) {
            for (FeeStructure fee : feeStructureRepository.findAllById(feeIds)) {
                feesById.put(fee.getId(), fee);
            }
        }

        Map<Long, List<Invoice>> byStudent = new HashMap<>();
        for (Invoice invoice : invoices) {
            byStudent.computeIfAbsent(invoice.getStudentId(), ignored -> new ArrayList<>()).add(invoice);
        }

        Long activeTermId = null;
        try {
            activeTermId = activeTermService.requireActiveTermId();
        } catch (ResourceNotFoundException | ConflictException ignored) {
            // No active term — parents cannot generate fees until an admin activates one.
        }

        List<ParentChildFeesResponse> children = new ArrayList<>();
        for (Long childId : childIds) {
            Student student = students.get(childId);
            if (student == null) {
                continue;
            }
            List<Invoice> childInvoices = byStudent.getOrDefault(childId, List.of());
            List<InvoiceResponse> items = childInvoices.stream()
                .map(inv -> {
                    FeeStructure fee = feesById.get(inv.getFeeStructureId());
                    if (fee == null || fee.getPublishedAt() == null || Boolean.FALSE.equals(fee.getIsActive())) {
                        return null;
                    }
                    return InvoiceResponse.from(
                        inv,
                        formatStudentName(student),
                        student.getAdmissionNumber(),
                        fee.getName(),
                        fee.getClassId(),
                        classNames.get(fee.getClassId()),
                        fee.getTermId(),
                        termNames.get(fee.getTermId()));
                })
                .filter(Objects::nonNull)
                .toList();

            BigDecimal total = items.stream()
                .map(InvoiceResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = items.stream()
                .filter(item -> item.status() == InvoiceStatus.PENDING)
                .map(InvoiceResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            int missingInvoiceCount = 0;
            if (activeTermId != null && student.getClassId() != null) {
                List<FeeStructure> publishedForClass = publishedActiveStructures(student.getClassId(), activeTermId);
                missingInvoiceCount = countMissingInvoices(student.getId(), publishedForClass);
            }

            children.add(new ParentChildFeesResponse(
                student.getId(),
                formatStudentName(student),
                student.getAdmissionNumber(),
                student.getClassId(),
                student.getClassId() == null ? null : classNames.get(student.getClassId()),
                total,
                outstanding,
                items,
                missingInvoiceCount > 0,
                missingInvoiceCount));
        }
        BigDecimal totalOutstanding = children.stream()
            .map(ParentChildFeesResponse::outstandingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ParentFeesResponse(totalOutstanding, children);
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        Student student = studentRepository.findById(invoice.getStudentId()).orElse(null);
        FeeStructure fee = findStructure(invoice.getFeeStructureId());
        return InvoiceResponse.from(
            invoice,
            student == null ? null : formatStudentName(student),
            student == null ? null : student.getAdmissionNumber(),
            fee.getName(),
            fee.getClassId(),
            classNameMap().get(fee.getClassId()),
            fee.getTermId(),
            termNameMap().get(fee.getTermId()));
    }

    private FeeStructureResponse toStructureResponse(FeeStructure fee) {
        return FeeStructureResponse.from(
            fee,
            classNameMap().get(fee.getClassId()),
            termNameMap().get(fee.getTermId()),
            invoiceRepository.countByFeeStructureId(fee.getId()));
    }

    private FeeTemplateResponse toTemplateResponse(FeeTemplate template, String className) {
        List<FeeTemplateItemResponse> items = template.getItems().stream()
            .map(item -> new FeeTemplateItemResponse(
                item.getId(),
                item.getName(),
                item.getDefaultAmount(),
                item.getDescription(),
                item.getSortOrder(),
                Boolean.TRUE.equals(item.getIsActive())))
            .toList();
        return new FeeTemplateResponse(template.getId(), template.getClassId(), className, items);
    }

    private FeeStructure findStructure(Long id) {
        return feeStructureRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee line item not found"));
    }

    private Map<Long, String> classNameMap() {
        return schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
    }

    private Map<Long, String> termNameMap() {
        return termRepository.findAll().stream()
            .collect(Collectors.toMap(Term::getId, Term::getName, (a, b) -> a));
    }

    private void validateClassAndTerm(Long classId, Long termId) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }
        if (!termRepository.existsById(termId)) {
            throw new ResourceNotFoundException("Term not found");
        }
    }

    private List<FeeStructure> publishedActiveStructures(Long classId, Long termId) {
        return feeStructureRepository
            .findByClassIdAndTermIdAndPublishedAtIsNotNullAndIsActiveTrueOrderByNameAsc(classId, termId);
    }

    private List<Student> activePupilsInClass(Long classId) {
        return studentRepository.findByClassId(classId).stream()
            .filter(s -> !Boolean.FALSE.equals(s.getIsActive()))
            .toList();
    }

    private int countMissingInvoices(Long studentId, List<FeeStructure> publishedStructures) {
        int missing = 0;
        for (FeeStructure fee : publishedStructures) {
            if (invoiceRepository.findByStudentIdAndFeeStructureId(studentId, fee.getId()).isEmpty()) {
                missing++;
            }
        }
        return missing;
    }

    private int createMissingInvoicesForStudent(Long studentId, List<FeeStructure> publishedStructures) {
        int invoicesCreated = 0;
        for (FeeStructure fee : publishedStructures) {
            if (invoiceRepository.findByStudentIdAndFeeStructureId(studentId, fee.getId()).isPresent()) {
                continue;
            }
            Invoice invoice = new Invoice();
            invoice.setStudentId(studentId);
            invoice.setFeeStructureId(fee.getId());
            invoice.setAmount(fee.getAmount());
            invoice.setDueDate(fee.getDueDate());
            invoice.setStatus(InvoiceStatus.PENDING);
            invoiceRepository.save(invoice);
            invoicesCreated++;
        }
        return invoicesCreated;
    }

    private static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
