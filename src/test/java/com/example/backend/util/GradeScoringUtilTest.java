package com.example.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.backend.entity.Grade;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GradeScoringUtilTest {

    @Test
    void computesAdditiveSubjectTotalAndTermAverage() {
        Grade ca = grade("CA", new BigDecimal("25"));
        Grade exam = grade("EXAM", new BigDecimal("55"));
        Set<String> types = Set.of("CA", "EXAM");

        assertEquals(new BigDecimal("80.00"), GradeScoringUtil.computeSubjectTotal(List.of(ca, exam), types));

        Grade otherSubjectCa = grade("CA", new BigDecimal("30"));
        otherSubjectCa.setSubjectId(2L);
        Grade otherSubjectExam = grade("EXAM", new BigDecimal("40"));
        otherSubjectExam.setSubjectId(2L);

        assertEquals(
            new BigDecimal("75.00"),
            GradeScoringUtil.computeTermAverage(List.of(ca, exam, otherSubjectCa, otherSubjectExam), types));
    }

    private static Grade grade(String type, BigDecimal score) {
        Grade grade = new Grade();
        grade.setSubjectId(1L);
        grade.setAssessmentType(type);
        grade.setScore(score);
        return grade;
    }
}
