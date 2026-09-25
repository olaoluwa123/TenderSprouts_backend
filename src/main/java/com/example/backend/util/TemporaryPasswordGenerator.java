package com.example.backend.util;

import java.security.SecureRandom;

/**
 * Generates temporary passwords for newly created portal accounts.
 * Format: {@code TSS} + random alphanumeric characters (ambiguous chars omitted).
 */
public final class TemporaryPasswordGenerator {

    private static final String PREFIX = "TSS";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;
    private static final int SUFFIX_LENGTH = 9;

    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        char[] suffix = new char[SUFFIX_LENGTH];
        suffix[0] = UPPER.charAt(RANDOM.nextInt(UPPER.length()));
        suffix[1] = LOWER.charAt(RANDOM.nextInt(LOWER.length()));
        suffix[2] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int i = 3; i < SUFFIX_LENGTH; i++) {
            suffix[i] = ALL.charAt(RANDOM.nextInt(ALL.length()));
        }
        shuffle(suffix);
        return PREFIX + new String(suffix);
    }

    private static void shuffle(char[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
}
