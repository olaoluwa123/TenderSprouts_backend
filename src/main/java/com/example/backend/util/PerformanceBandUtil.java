package com.example.backend.util;

import java.awt.Color;
import java.math.BigDecimal;

/**
 * Tender Sprouts end-of-term bands from the school result template.
 */
public final class PerformanceBandUtil {

    public static final String EXCELLENT = "EXCELLENT";
    public static final String VERY_GOOD = "VERY GOOD";
    public static final String GOOD = "GOOD";
    public static final String SATISFACTORY = "SATISFACTORY";
    public static final String AVERAGE = "AVERAGE";
    public static final String BELOW_AVERAGE = "BELOW AVERAGE";

    public static final Color YELLOW = new Color(0xF6, 0xE0, 0x4D);
    public static final Color GREEN = new Color(0x7C, 0xB3, 0x42);
    public static final Color LIME = new Color(0xC6, 0xFF, 0x00);
    public static final Color ORANGE = new Color(0xFF, 0xB7, 0x4D);
    public static final Color CYAN = new Color(0x4D, 0xD0, 0xE1);
    public static final Color PINK = new Color(0xF4, 0x8F, 0xB1);
    public static final Color RED = new Color(0xEF, 0x53, 0x50);
    public static final Color DARK_RED = new Color(0xC6, 0x28, 0x28);
    public static final Color HEADER_RED = new Color(0xC6, 0x28, 0x28);
    public static final Color HEADER_GREEN = new Color(0x2E, 0x7D, 0x32);
    public static final Color HEADER_BLUE = new Color(0x15, 0x65, 0xC0);
    public static final Color PAGE_PINK = new Color(0xEC, 0x9A, 0xB0);

    private PerformanceBandUtil() {
    }

    public static String toBand(BigDecimal total) {
        if (total == null) {
            return null;
        }
        double score = total.doubleValue();
        if (score >= 90) {
            return EXCELLENT;
        }
        if (score >= 80) {
            return VERY_GOOD;
        }
        if (score >= 70) {
            return GOOD;
        }
        if (score >= 60) {
            return SATISFACTORY;
        }
        if (score >= 50) {
            return AVERAGE;
        }
        return BELOW_AVERAGE;
    }

    public static Color bandColor(String band) {
        if (band == null) {
            return Color.WHITE;
        }
        return switch (band) {
            case EXCELLENT -> GREEN;
            case VERY_GOOD -> LIME;
            case GOOD -> YELLOW;
            case SATISFACTORY -> ORANGE;
            case AVERAGE -> RED;
            case BELOW_AVERAGE -> DARK_RED;
            default -> Color.WHITE;
        };
    }

    public static Color bandTextColor(String band) {
        if (AVERAGE.equals(band) || BELOW_AVERAGE.equals(band)) {
            return Color.WHITE;
        }
        return Color.BLACK;
    }
}
