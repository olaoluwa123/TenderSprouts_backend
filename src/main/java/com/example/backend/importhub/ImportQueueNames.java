package com.example.backend.importhub;

public final class ImportQueueNames {

    public static final String EXCHANGE = "import";
    public static final String ROUTING_STUDENTS = "students";
    public static final String ROUTING_STUDENTS_DLQ = "students.dlq";
    public static final String ROUTING_TEACHERS = "teachers";
    public static final String ROUTING_TEACHERS_DLQ = "teachers.dlq";

    public static final String QUEUE_STUDENTS = "import.students";
    public static final String QUEUE_STUDENTS_DLQ = "import.students.dlq";
    public static final String QUEUE_TEACHERS = "import.teachers";
    public static final String QUEUE_TEACHERS_DLQ = "import.teachers.dlq";

    private ImportQueueNames() {
    }
}
