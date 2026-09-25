package com.example.backend.whatsapp;

public final class WhatsAppQueueNames {

    public static final String EXCHANGE = "whatsapp";
    public static final String ROUTING_SEND = "send";
    public static final String ROUTING_RETRY = "retry";
    public static final String ROUTING_DLQ = "dlq";

    public static final String QUEUE_SEND = "whatsapp.send";
    public static final String QUEUE_RETRY = "whatsapp.send.retry";
    public static final String QUEUE_DLQ = "whatsapp.send.dlq";

    /** 10 minutes */
    public static final int RETRY_DELAY_MS = 10 * 60 * 1000;

    private WhatsAppQueueNames() {
    }
}
