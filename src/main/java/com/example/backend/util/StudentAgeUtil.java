package com.example.backend.util;

import java.time.LocalDate;
import java.time.Period;

public final class StudentAgeUtil {

    private StudentAgeUtil() {
    }

    public static Integer yearsAt(LocalDate dateOfBirth, LocalDate onDate) {
        if (dateOfBirth == null || onDate == null) {
            return null;
        }
        int years = Period.between(dateOfBirth, onDate).getYears();
        return years < 0 ? null : years;
    }

    public static Integer yearsNow(LocalDate dateOfBirth) {
        return yearsAt(dateOfBirth, LocalDate.now());
    }
}
