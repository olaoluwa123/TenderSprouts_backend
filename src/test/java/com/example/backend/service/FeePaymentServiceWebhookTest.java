package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.backend.config.PaystackProperties;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.InvoiceRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class FeePaymentServiceWebhookTest {

    private PaymentRepository paymentRepository;
    private FeePaymentService service;

    @BeforeEach
    void setUp() {
        paymentRepository = Mockito.mock(PaymentRepository.class);
        service = new FeePaymentService(
            configuredPaystack(),
            new AcceptingPaystackClient(),
            Mockito.mock(InvoiceRepository.class),
            paymentRepository,
            Mockito.mock(ParentRepository.class),
            Mockito.mock(ParentStudentRepository.class),
            new UnusedFeeService());
    }

    @Test
    void unknownReferenceDoesNotAcknowledgeWebhook() {
        when(paymentRepository.findByGatewayReference(any())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
            ResourceNotFoundException.class,
            () -> service.handleWebhook(
                "sig",
                "{\"event\":\"charge.success\",\"data\":{\"reference\":\"TSP-P3-missing\"}}"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Payment not found", ex.getMessage());
    }

    @Test
    void blankReferenceDoesNotAcknowledgeWebhook() {
        ResourceNotFoundException ex = assertThrows(
            ResourceNotFoundException.class,
            () -> service.handleWebhook(
                "sig",
                "{\"event\":\"charge.success\",\"data\":{\"reference\":\"\"}}"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Payment not found", ex.getMessage());
    }

    private static PaystackProperties configuredPaystack() {
        PaystackProperties properties = new PaystackProperties();
        properties.setEnabled(true);
        properties.setSecretKey("sk_test_dummy");
        properties.setPublicKey("pk_test_dummy");
        return properties;
    }

    private static final class AcceptingPaystackClient extends PaystackClient {
        private AcceptingPaystackClient() {
            super(configuredPaystack(), new ObjectMapper());
        }

        @Override
        public boolean isValidWebhookSignature(String signature, String rawBody) {
            return true;
        }

    }

    private static final class UnusedFeeService extends FeeService {
        private UnusedFeeService() {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }
}
