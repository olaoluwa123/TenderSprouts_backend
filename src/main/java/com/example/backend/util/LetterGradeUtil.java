package com.example.backend.util;

import java.math.BigDecimal;

public final class LetterGradeUtil {

    private LetterGradeUtil() {
    }

    public static String toLetter(BigDecimal total) {
        if (total == null) {
            return null;
        }
        int score = total.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        if (score >= 70) {
            return "A";
        }
        if (score >= 60) {
            return "B";
        }
        if (score >= 50) {
            return "C";
        }
        if (score >= 45) {
            return "D";
        }
        if (score >= 40) {
            return "E";
        }
        return "F";
    }
}
