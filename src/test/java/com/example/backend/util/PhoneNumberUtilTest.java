package com.example.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PhoneNumberUtilTest {

    @Test
    void normalizesNigerianLocalMobile() {
        assertEquals("2348012345678", PhoneNumberUtil.toE164("08012345678"));
        assertEquals("2348012345678", PhoneNumberUtil.toE164("0801 234 5678"));
        assertEquals("2348012345678", PhoneNumberUtil.toE164("+2348012345678"));
        assertEquals("2348012345678", PhoneNumberUtil.toE164("2348012345678"));
        assertEquals("2348012345678", PhoneNumberUtil.toE164("8012345678"));
    }

    @Test
    void rejectsBlankAndInvalid() {
        assertNull(PhoneNumberUtil.toE164(null));
        assertNull(PhoneNumberUtil.toE164("   "));
        assertNull(PhoneNumberUtil.toE164("123"));
        assertNull(PhoneNumberUtil.toE164("abc"));
    }
}
