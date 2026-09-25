package com.example.backend.util;

/**
 * Normalizes phone numbers to WhatsApp Cloud API format (digits only, E.164 without '+').
 * Defaults to Nigeria (234) when a local 0-prefixed mobile number is provided.
 */
public final class PhoneNumberUtil {

    private static final String DEFAULT_COUNTRY_CODE = "234";

    private PhoneNumberUtil() {
    }

    /**
     * @return digits-only international number (e.g. {@code 2348012345678}), or {@code null} if invalid
     */
    public static String toE164(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        String digits;
        if (trimmed.startsWith("+")) {
            digits = trimmed.substring(1).replaceAll("\\D", "");
        } else if (trimmed.startsWith("00")) {
            digits = trimmed.substring(2).replaceAll("\\D", "");
        } else {
            digits = trimmed.replaceAll("\\D", "");
        }
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.startsWith("0") && digits.length() >= 10 && digits.length() <= 11) {
            digits = DEFAULT_COUNTRY_CODE + digits.substring(1);
        } else if (digits.length() == 10
            && (digits.startsWith("7") || digits.startsWith("8") || digits.startsWith("9"))) {
            // Nigerian mobile without leading 0
            digits = DEFAULT_COUNTRY_CODE + digits;
        }
        if (digits.length() < 10 || digits.length() > 15) {
            return null;
        }
        return digits;
    }
}
