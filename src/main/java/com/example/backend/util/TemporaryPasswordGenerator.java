package com.example.backend.util;

import java.security.SecureRandom;

public final class TemporaryPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;
    private static final int DEFAULT_LENGTH = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    static String generate(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4");
        }
        char[] password = new char[length];
        password[0] = UPPER.charAt(RANDOM.nextInt(UPPER.length()));
        password[1] = LOWER.charAt(RANDOM.nextInt(LOWER.length()));
        password[2] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int i = 3; i < length; i++) {
            password[i] = ALL.charAt(RANDOM.nextInt(ALL.length()));
        }
        shuffle(password);
        return new String(password);
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
