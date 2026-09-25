package com.example.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StudentAgeUtilTest {

    @Test
    void yearsAtComputesCompletedYears() {
        assertEquals(6, StudentAgeUtil.yearsAt(LocalDate.of(2018, 3, 12), LocalDate.of(2024, 8, 17)));
        assertEquals(5, StudentAgeUtil.yearsAt(LocalDate.of(2018, 8, 20), LocalDate.of(2024, 8, 17)));
    }

    @Test
    void yearsAtReturnsNullWhenMissing() {
        assertNull(StudentAgeUtil.yearsAt(null, LocalDate.now()));
        assertNull(StudentAgeUtil.yearsAt(LocalDate.of(2018, 1, 1), null));
    }
}
