package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.backend.config.PaystackProperties;
import com.example.backend.dto.fee.InitializePaymentResponse;
import com.example.backend.entity.Invoice;
import com.example.backend.entity.Payment;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.PaymentStatus;
import com.example.backend.repository.InvoiceRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.security.SecurityUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FeePaymentServiceInitializeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<Payment> stored = new AtomicReference<>();
    private final AtomicInteger initializeCalls = new AtomicInteger();

    private InvoiceRepository invoiceRepository;
    private PaymentRepository paymentRepository;
    private RecordingPaystackClient paystackClient;
    private FeePaymentService service;

    @BeforeEach
    void setUp() {
        stored.set(null);
        initializeCalls.set(0);
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        paymentRepository = Mockito.mock(PaymentRepository.class);
        paystackClient = new RecordingPaystackClient(initializeCalls, objectMapper);

        when(invoiceRepository.findAllByIdForUpdate(any())).thenReturn(List.of());
        when(paymentRepository.findByInvoiceIdAndStatus(any(), eq(PaymentStatus.PENDING)))
            .thenAnswer(invocation -> stored.get() == null ? List.of() : List.of(stored.get()));
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(99L);
            }
            stored.set(payment);
            return payment;
        });

        service = new FeePaymentService(
            configuredPaystack(),
            paystackClient,
            invoiceRepository,
            paymentRepository,
            Mockito.mock(ParentRepository.class),
            Mockito.mock(ParentStudentRepository.class),
            new StubFeeService());
    }

    @Test
    void secondInitializeReusesPendingPaystackCharge() {
        SecurityUser parent = new SecurityUser(5L, "parent@email.com", "x", User.Role.PARENT, 3L);

        InitializePaymentResponse first = service.initializeForParent(parent, 1L, 2L);
        InitializePaymentResponse second = service.initializeForParent(parent, 1L, 2L);

        assertEquals("acc_reuse", first.accessCode());
        assertEquals(first.accessCode(), second.accessCode());
        assertEquals(first.reference(), second.reference());
        assertEquals(List.of(10L, 11L), first.invoiceIds());
        assertEquals(1, initializeCalls.get());
    }

    private static PaystackProperties configuredPaystack() {
        PaystackProperties properties = new PaystackProperties();
        properties.setEnabled(true);
        properties.setSecretKey("sk_test_dummy");
        properties.setPublicKey("pk_test_dummy");
        return properties;
    }

    private static Invoice invoice(Long id, String amount) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setAmount(new BigDecimal(amount));
        invoice.setStudentId(1L);
        return invoice;
    }

    private static final class RecordingPaystackClient extends PaystackClient {
        private final AtomicInteger initializeCalls;
        private final ObjectMapper objectMapper;

        private RecordingPaystackClient(AtomicInteger initializeCalls, ObjectMapper objectMapper) {
            super(configuredPaystack(), objectMapper);
            this.initializeCalls = initializeCalls;
            this.objectMapper = objectMapper;
        }

        @Override
        public JsonNode initializeTransaction(
            String email,
            long amountKobo,
            String reference,
            Map<String, Object> metadata) {
            initializeCalls.incrementAndGet();
            try {
                return objectMapper.readTree("{\"access_code\":\"acc_reuse\"}");
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static final class StubFeeService extends FeeService {
        private StubFeeService() {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public List<Invoice> pendingPayableInvoicesForParent(SecurityUser actor, Long sessionId, Long termId) {
            return List.of(invoice(11L, "15000.00"), invoice(10L, "5000.00"));
        }
    }
}
