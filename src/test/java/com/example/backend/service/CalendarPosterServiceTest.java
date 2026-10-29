package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CalendarPosterServiceTest {

    @Test
    void detectJpegPngGifWebpAndRejectOther() {
        byte[] jpeg = padded(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
        byte[] png = padded(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        byte[] gif = padded(new byte[] {'G', 'I', 'F', '8', '9', 'a'});
        byte[] webp = padded(new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'});
        assertEquals(CalendarPosterService.ImageKind.JPEG, CalendarPosterService.detectImage(jpeg));
        assertEquals(CalendarPosterService.ImageKind.PNG, CalendarPosterService.detectImage(png));
        assertEquals(CalendarPosterService.ImageKind.GIF, CalendarPosterService.detectImage(gif));
        assertEquals(CalendarPosterService.ImageKind.WEBP, CalendarPosterService.detectImage(webp));
        assertNull(CalendarPosterService.detectImage("not an image!!".getBytes()));
        assertNull(CalendarPosterService.detectImage("<svg xmlns='http://www.w3.org/2000/svg'></svg>".getBytes()));
    }

    @Test
    void sanitizeFilenameStripsPathAndUnsafeCharacters() {
        assertEquals("first_term_calendar.jpg",
            CalendarPosterService.sanitizeFilename("C:\\Users\\Admin\\First Term Calendar.jpg"));
        assertEquals("calendar-poster", CalendarPosterService.sanitizeFilename("   "));
    }

    private static byte[] padded(byte[] prefix) {
        byte[] out = new byte[Math.max(12, prefix.length)];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        return out;
    }
}
