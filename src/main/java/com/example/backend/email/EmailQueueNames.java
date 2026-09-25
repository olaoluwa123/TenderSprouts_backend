package com.example.backend.email;

public final class EmailQueueNames {

    public static final String EXCHANGE = "email";
    public static final String ROUTING_SEND = "send";
    public static final String ROUTING_RETRY = "retry";
    public static final String ROUTING_DLQ = "dlq";

    public static final String QUEUE_SEND = "email.send";
    public static final String QUEUE_RETRY = "email.send.retry";
    public static final String QUEUE_DLQ = "email.send.dlq";

    /** 10 minutes */
    public static final int RETRY_DELAY_MS = 10 * 60 * 1000;

    private EmailQueueNames() {
    }
}
