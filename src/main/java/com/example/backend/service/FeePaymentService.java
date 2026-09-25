package com.example.backend.service;

import com.example.backend.config.PaystackProperties;
import com.example.backend.dto.fee.InitializePaymentResponse;
import com.example.backend.dto.fee.ParentFeesResponse;
import com.example.backend.entity.Invoice;
import com.example.backend.entity.Parent;
import com.example.backend.entity.Payment;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.InvoiceStatus;
import com.example.backend.entity.enums.PaymentStatus;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.InvoiceRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.security.SecurityUser;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeePaymentService {

    private final PaystackProperties paystackProperties;
    private final PaystackClient paystackClient;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final FeeService feeService;

    public FeePaymentService(
        PaystackProperties paystackProperties,
        PaystackClient paystackClient,
        InvoiceRepository invoiceRepository,
        PaymentRepository paymentRepository,
        ParentRepository parentRepository,
        ParentStudentRepository parentStudentRepository,
        FeeService feeService) {
        this.paystackProperties = paystackProperties;
        this.paystackClient = paystackClient;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.parentRepository = parentRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.feeService = feeService;
    }

    @Transactional
    public InitializePaymentResponse initializeForParent(
        SecurityUser actor,
        Long sessionId,
        Long termId) {
        if (!paystackProperties.isConfigured()) {
            throw new BadRequestException(
                "Online payments are not enabled yet. Ask the school to configure Paystack.");
        }
        List<Invoice> invoices = feeService.pendingPayableInvoicesForParent(actor, sessionId, termId);
        if (invoices.isEmpty()) {
            throw new BadRequestException("No outstanding fees to pay");
        }

        List<Long> invoiceIds = invoices.stream().map(Invoice::getId).sorted().toList();
        invoiceRepository.findAllByIdForUpdate(invoiceIds);

        BigDecimal total = invoices.stream()
            .map(Invoice::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Payment payment = findReusablePendingPayment(invoiceIds, total);
        boolean reused = payment != null;
        if (payment == null) {
            payment = new Payment();
            payment.setInvoiceIds(invoiceIds);
            payment.setInvoiceId(invoiceIds.get(0));
            payment.setAmount(total);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setGatewayReference(newGatewayReference(actor));
            payment = paymentRepository.save(payment);
        }

        String accessCode = reused
            ? ensureAccessCode(payment, actor, invoiceIds, total)
            : initializeAccessCode(payment, actor, invoiceIds, total);
        payment.setGatewayAccessCode(accessCode);
        paymentRepository.save(payment);

        return new InitializePaymentResponse(
            accessCode,
            payment.getGatewayReference(),
            payment.getAmount(),
            invoiceIds);
    }

    @Transactional
    public ParentFeesResponse verify(String reference, SecurityUser actor) {
        if (reference == null || reference.isBlank()) {
            throw new BadRequestException("Payment reference is required");
        }
        Payment payment = paymentRepository.findByGatewayReference(reference.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        List<Long> invoiceIds = paymentInvoiceIds(payment);
        if (invoiceIds.isEmpty()) {
            throw new BadRequestException("Payment has no linked invoices");
        }
        List<Invoice> invoices = invoiceRepository.findAllById(invoiceIds);
        for (Invoice invoice : invoices) {
            requireParentOwnsStudent(actor, invoice.getStudentId());
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS && allInvoicesPaid(invoices)) {
            return feeService.parentFees(actor);
        }

        JsonNode root = paystackClient.verifyTransaction(reference.trim());
        applySuccessfulCharge(payment, invoices, root, reference.trim());
        return feeService.parentFees(actor);
    }

    @Transactional
    public void handleWebhook(String signature, String rawBody) {
        if (!paystackClient.isValidWebhookSignature(signature, rawBody)) {
            throw new ForbiddenException("Invalid Paystack signature");
        }
        JsonNode root = paystackClient.parseJson(rawBody);
        String event = text(root, "event");
        if (!"charge.success".equalsIgnoreCase(event)) {
            return;
        }
        JsonNode data = root.path("data");
        String reference = text(data, "reference");
        if (reference == null || reference.isBlank()) {
            throw new ResourceNotFoundException("Payment not found");
        }
        Payment payment = paymentRepository.findByGatewayReference(reference)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        List<Long> invoiceIds = paymentInvoiceIds(payment);
        if (invoiceIds.isEmpty()) {
            throw new ResourceNotFoundException("Payment not found");
        }
        List<Invoice> invoices = invoiceRepository.findAllById(invoiceIds);
        if (payment.getStatus() == PaymentStatus.SUCCESS && allInvoicesPaid(invoices)) {
            return;
        }
        applySuccessfulCharge(payment, invoices, root, reference);
    }

    private void applySuccessfulCharge(
        Payment payment,
        List<Invoice> invoices,
        JsonNode verifyOrWebhookRoot,
        String reference) {
        JsonNode data = verifyOrWebhookRoot.has("data") ? verifyOrWebhookRoot.path("data") : verifyOrWebhookRoot;
        String status = text(data, "status");
        if (status == null || !"success".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Payment was not successful");
        }

        long paidKobo = data.path("amount").asLong(-1);
        long expectedKobo = toKobo(payment.getAmount());
        if (paidKobo != expectedKobo) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Paid amount does not match outstanding fees");
        }

        String currency = text(data, "currency");
        if (currency != null && !"NGN".equalsIgnoreCase(currency)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Unexpected payment currency");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        feeService.markInvoicesPaidFromGateway(
            invoices.stream().map(Invoice::getId).toList(),
            reference);
    }

    private Payment findReusablePendingPayment(List<Long> invoiceIds, BigDecimal total) {
        Set<Long> seen = new HashSet<>();
        for (Long invoiceId : invoiceIds) {
            for (Payment payment : paymentRepository.findByInvoiceIdAndStatus(invoiceId, PaymentStatus.PENDING)) {
                if (payment.getId() != null && !seen.add(payment.getId())) {
                    continue;
                }
                if (matchesInvoiceSetAndAmount(payment, invoiceIds, total)) {
                    return payment;
                }
            }
        }
        return null;
    }

    private boolean matchesInvoiceSetAndAmount(Payment payment, List<Long> invoiceIds, BigDecimal total) {
        List<Long> existing = new ArrayList<>(paymentInvoiceIds(payment));
        Collections.sort(existing);
        if (!existing.equals(invoiceIds)) {
            return false;
        }
        return payment.getAmount() != null && payment.getAmount().compareTo(total) == 0;
    }

    private String ensureAccessCode(
        Payment payment,
        SecurityUser actor,
        List<Long> invoiceIds,
        BigDecimal total) {
        String stored = payment.getGatewayAccessCode();
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        String recovered = recoverAccessCode(payment.getGatewayReference());
        if (recovered != null && !recovered.isBlank()) {
            return recovered;
        }
        return initializeAccessCode(payment, actor, invoiceIds, total);
    }

    private String recoverAccessCode(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        try {
            JsonNode root = paystackClient.verifyTransaction(reference);
            JsonNode data = root.has("data") ? root.path("data") : root;
            return text(data, "access_code");
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String initializeAccessCode(
        Payment payment,
        SecurityUser actor,
        List<Long> invoiceIds,
        BigDecimal total) {
        String reference = payment.getGatewayReference();
        if (reference == null || reference.isBlank()) {
            reference = newGatewayReference(actor);
            payment.setGatewayReference(reference);
        }
        Map<String, Object> metadata = Map.of(
            "invoiceIds", invoiceIds,
            "parentId", actor.getProfileId(),
            "accountEmail", actor.getUsername() == null ? "" : actor.getUsername()
        );
        try {
            return accessCodeFromInitialize(actor, total, reference, metadata);
        } catch (BadRequestException ex) {
            if (!isDuplicateReference(ex.getMessage())) {
                throw ex;
            }
            reference = newGatewayReference(actor);
            payment.setGatewayReference(reference);
            return accessCodeFromInitialize(actor, total, reference, metadata);
        }
    }

    private String accessCodeFromInitialize(
        SecurityUser actor,
        BigDecimal total,
        String reference,
        Map<String, Object> metadata) {
        JsonNode data = paystackClient.initializeTransaction(
            paystackCustomerEmail(actor),
            toKobo(total),
            reference,
            metadata);
        String accessCode = text(data, "access_code");
        if (accessCode == null || accessCode.isBlank()) {
            throw new BadRequestException("Paystack did not return an access code");
        }
        return accessCode;
    }

    private static String newGatewayReference(SecurityUser actor) {
        return "TSP-P" + actor.getProfileId() + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static boolean isDuplicateReference(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("duplicate") && lower.contains("reference");
    }

    private List<Long> paymentInvoiceIds(Payment payment) {
        if (payment.getInvoiceIds() != null && !payment.getInvoiceIds().isEmpty()) {
            return payment.getInvoiceIds();
        }
        if (payment.getInvoiceId() != null) {
            return List.of(payment.getInvoiceId());
        }
        return List.of();
    }

    private static boolean allInvoicesPaid(List<Invoice> invoices) {
        return !invoices.isEmpty()
            && invoices.stream().allMatch(invoice -> invoice.getStatus() == InvoiceStatus.PAID);
    }

    private void requireParentOwnsStudent(SecurityUser actor, Long studentId) {
        if (actor.getRole() != User.Role.PARENT || actor.getProfileId() == null) {
            throw new ForbiddenException("Parent profile required");
        }
        Parent parent = parentRepository.findById(actor.getProfileId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        boolean linked = parentStudentRepository.existsByParentIdAndStudentId(parent.getId(), studentId);
        if (!linked) {
            throw new ForbiddenException("Not linked to this student");
        }
    }

    private static long toKobo(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * Paystack rejects reserved TLDs like {@code .local}. Use a valid stand-in for local seed accounts.
     */
    private static String paystackCustomerEmail(SecurityUser actor) {
        String email = actor.getUsername();
        if (isPaystackCompatibleEmail(email)) {
            return email.trim();
        }
        Long userId = actor.getUserId() == null ? 0L : actor.getUserId();
        return "parent" + userId + "@email.com";
    }

    private static boolean isPaystackCompatibleEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return false;
        }
        String lower = email.trim().toLowerCase(Locale.ROOT);
        return !(lower.endsWith(".local")
            || lower.endsWith(".test")
            || lower.endsWith(".invalid")
            || lower.endsWith(".example")
            || lower.endsWith(".localhost"));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
