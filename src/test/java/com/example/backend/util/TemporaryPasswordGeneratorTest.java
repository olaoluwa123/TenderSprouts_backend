package com.example.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9]+$");

    @Test
    void generate_defaultLengthAndCharset() {
        String password = TemporaryPasswordGenerator.generate();
        assertEquals(12, password.length());
        assertTrue(ALLOWED.matcher(password).matches());
        assertTrue(UPPER.matcher(password).find());
        assertTrue(LOWER.matcher(password).find());
        assertTrue(DIGIT.matcher(password).find());
        assertTrue(password.chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0));
    }

    @Test
    void generate_producesUniqueValues() {
        String first = TemporaryPasswordGenerator.generate();
        String second = TemporaryPasswordGenerator.generate();
        assertTrue(!first.equals(second) || first.length() == second.length());
    }
}
