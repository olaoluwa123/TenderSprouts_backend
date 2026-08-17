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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public InitializePaymentResponse initializeForParent(SecurityUser actor) {
        if (!paystackProperties.isConfigured()) {
            throw new BadRequestException(
                "Online payments are not enabled yet. Ask the school to configure Paystack.");
        }
        List<Invoice> invoices = feeService.pendingPayableInvoicesForParent(actor);
        if (invoices.isEmpty()) {
            throw new BadRequestException("No outstanding fees to pay");
        }

        List<Long> invoiceIds = invoices.stream().map(Invoice::getId).toList();
        BigDecimal total = invoices.stream()
            .map(Invoice::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String reference = "TSP-P" + actor.getProfileId() + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Payment payment = new Payment();
        payment.setInvoiceIds(invoiceIds);
        payment.setInvoiceId(invoiceIds.get(0));
        payment.setAmount(total);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayReference(reference);
        paymentRepository.save(payment);

        long amountKobo = toKobo(total);
        String paystackEmail = paystackCustomerEmail(actor);
        JsonNode data = paystackClient.initializeTransaction(
            paystackEmail,
            amountKobo,
            reference,
            Map.of(
                "invoiceIds", invoiceIds,
                "parentId", actor.getProfileId(),
                "accountEmail", actor.getUsername() == null ? "" : actor.getUsername()
            ));
        String accessCode = text(data, "access_code");
        if (accessCode == null || accessCode.isBlank()) {
            throw new BadRequestException("Paystack did not return an access code");
        }
        return new InitializePaymentResponse(accessCode, reference, total, invoiceIds);
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
            return;
        }
        Payment payment = paymentRepository.findByGatewayReference(reference).orElse(null);
        if (payment == null) {
            return;
        }
        List<Long> invoiceIds = paymentInvoiceIds(payment);
        if (invoiceIds.isEmpty()) {
            return;
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
