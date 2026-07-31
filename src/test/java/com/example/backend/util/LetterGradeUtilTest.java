package com.example.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LetterGradeUtilTest {

    @Test
    void mapsBoundaryScoresToLetterGrades() {
        assertEquals("F", LetterGradeUtil.toLetter(new BigDecimal("39")));
        assertEquals("E", LetterGradeUtil.toLetter(new BigDecimal("40")));
        assertEquals("D", LetterGradeUtil.toLetter(new BigDecimal("45")));
        assertEquals("C", LetterGradeUtil.toLetter(new BigDecimal("50")));
        assertEquals("B", LetterGradeUtil.toLetter(new BigDecimal("60")));
        assertEquals("A", LetterGradeUtil.toLetter(new BigDecimal("70")));
        assertEquals("A", LetterGradeUtil.toLetter(new BigDecimal("100")));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(LetterGradeUtil.toLetter(null));
    }
}
