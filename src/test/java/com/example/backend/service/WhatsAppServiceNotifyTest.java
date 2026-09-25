package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.config.AppWhatsAppProperties;
import com.example.backend.whatsapp.WhatsAppCloudClient;
import com.example.backend.whatsapp.WhatsAppDocument;
import com.example.backend.whatsapp.WhatsAppJob;
import com.example.backend.whatsapp.WhatsAppPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

class WhatsAppServiceNotifyTest {

    private final List<WhatsAppJob> published = new ArrayList<>();
    private WhatsAppService whatsAppService;

    @BeforeEach
    void setUp() {
        published.clear();
        AppWhatsAppProperties properties = new AppWhatsAppProperties();
        properties.setEnabled(false);
        whatsAppService = new WhatsAppService(new CapturingWhatsAppPublisher(published::add), properties);
    }

    @Test
    void sendFeesPublishedEnqueuesTemplateJob() {
        whatsAppService.sendFeesPublished(
            "2348012345678",
            "Mrs Parent",
            "Ada Okonkwo",
            "JSS 1A",
            "First Term",
            "http://localhost:3000/parent/fees");

        assertEquals(1, published.size());
        WhatsAppJob job = published.get(0);
        assertEquals("2348012345678", job.toE164());
        assertEquals("fees_published", job.templateName());
        assertEquals(List.of(
            "Mrs Parent",
            "First Term",
            "JSS 1A",
            "Ada Okonkwo",
            "http://localhost:3000/parent/fees"), job.bodyParams());
        assertTrue(job.documents().isEmpty());
    }

    @Test
    void sendTermResultsPublishedIncludesPdfDocument() {
        byte[] pdf = new byte[] {37, 80, 68, 70};
        WhatsAppDocument document = new WhatsAppDocument(
            "ReportCard_CSV001_First_Term.pdf",
            "application/pdf",
            pdf);

        whatsAppService.sendTermResultsPublished(
            "2348012345678",
            "Mrs Parent",
            "Ada Okonkwo",
            "First Term",
            "http://localhost:3000/parent/results",
            List.of(document));

        assertEquals(1, published.size());
        WhatsAppJob job = published.get(0);
        assertEquals("results_published", job.templateName());
        assertEquals(1, job.documents().size());
        assertEquals("application/pdf", job.documents().get(0).mimeType());
        assertEquals(4, job.documents().get(0).content().length);
    }

    @Test
    void disabledCloudClientTreatsSendAsSuccess() {
        AppWhatsAppProperties properties = new AppWhatsAppProperties();
        properties.setEnabled(false);
        WhatsAppCloudClient client = new WhatsAppCloudClient(properties, new ObjectMapper());
        boolean ok = client.send(WhatsAppJob.initial(
            "2348012345678",
            "fees_published",
            List.of("a", "b", "c", "d", "e")));
        assertTrue(ok);
    }

    private static final class CapturingWhatsAppPublisher extends WhatsAppPublisher {
        private final Consumer<WhatsAppJob> sink;

        private CapturingWhatsAppPublisher(Consumer<WhatsAppJob> sink) {
            super(
                nullProvider(),
                new WhatsAppCloudClient(new AppWhatsAppProperties(), new ObjectMapper()));
            this.sink = sink;
        }

        @Override
        public void publish(WhatsAppJob job) {
            sink.accept(job);
        }

        private static <T> ObjectProvider<T> nullProvider() {
            return new ObjectProvider<>() {
                @Override
                public T getObject() throws BeansException {
                    return null;
                }

                @Override
                public T getObject(Object... args) throws BeansException {
                    return null;
                }

                @Override
                public T getIfAvailable() throws BeansException {
                    return null;
                }

                @Override
                public T getIfUnique() throws BeansException {
                    return null;
                }
            };
        }
    }
}
