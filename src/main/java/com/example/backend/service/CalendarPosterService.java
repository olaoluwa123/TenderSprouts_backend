package com.example.backend.service;

import com.example.backend.dto.calendar.CalendarPosterResponse;
import com.example.backend.entity.SchoolCalendarPoster;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.SchoolCalendarPosterRepository;
import com.example.backend.repository.SchoolCalendarPosterRepository.CalendarPosterMetadataView;
import com.example.backend.security.SecurityUser;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CalendarPosterService {

    static final long MAX_BYTES = 8L * 1024 * 1024;

    private final SchoolCalendarPosterRepository posterRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public CalendarPosterService(
        SchoolCalendarPosterRepository posterRepository,
        AuthorizationService authorizationService,
        AuditService auditService) {
        this.posterRepository = posterRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    public CalendarPosterResponse metadata() {
        return posterRepository.findMetadataById(SchoolCalendarPoster.SINGLETON_ID)
            .map(this::toResponse)
            .orElseGet(CalendarPosterResponse::missing);
    }

    public PosterFile getFile() {
        SchoolCalendarPoster poster = posterRepository.findById(SchoolCalendarPoster.SINGLETON_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Calendar poster not found"));
        return new PosterFile(
            sanitizeFilename(poster.getOriginalFilename()),
            poster.getContentType(),
            poster.getFileBytes());
    }

    @Transactional
    public CalendarPosterResponse upload(MultipartFile file, SecurityUser actor) {
        authorizationService.requireAdmin();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose an image to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image must be 8MB or smaller");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the uploaded image");
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new BadRequestException("Image must be 8MB or smaller");
        }
        ImageKind kind = detectImage(bytes);
        if (kind == null) {
            throw new BadRequestException("Upload a JPG, PNG, WEBP, or GIF image");
        }
        String filename = sanitizeFilename(file.getOriginalFilename());
        if (filename.equals("calendar-poster")) {
            filename = "calendar-poster" + kind.extension();
        }

        SchoolCalendarPoster poster = posterRepository.findById(SchoolCalendarPoster.SINGLETON_ID)
            .orElseGet(() -> {
                SchoolCalendarPoster created = new SchoolCalendarPoster();
                created.setId(SchoolCalendarPoster.SINGLETON_ID);
                return created;
            });
        poster.setOriginalFilename(filename);
        poster.setContentType(kind.contentType());
        poster.setFileBytes(bytes);
        poster.setUploadedByUserId(actor.getUserId());
        SchoolCalendarPoster saved = posterRepository.save(poster);
        auditService.logAdminAction(actor, "UPDATE", "calendar_poster", saved.getId(), filename);
        return toResponse(saved.getOriginalFilename(), saved.getContentType(),
            Optional.ofNullable(saved.getUpdatedAt()).orElse(saved.getCreatedAt()));
    }

    @Transactional
    public void delete(SecurityUser actor) {
        authorizationService.requireAdmin();
        posterRepository.findById(SchoolCalendarPoster.SINGLETON_ID).ifPresent(poster -> {
            posterRepository.delete(poster);
            auditService.logAdminAction(actor, "DELETE", "calendar_poster", poster.getId(), poster.getOriginalFilename());
        });
    }

    private CalendarPosterResponse toResponse(CalendarPosterMetadataView view) {
        LocalDateTime uploadedAt = view.getUpdatedAt() != null ? view.getUpdatedAt() : view.getCreatedAt();
        return toResponse(view.getOriginalFilename(), view.getContentType(), uploadedAt);
    }

    private CalendarPosterResponse toResponse(String filename, String contentType, LocalDateTime uploadedAt) {
        return CalendarPosterResponse.present(filename, contentType, uploadedAt);
    }

    static ImageKind detectImage(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
            return ImageKind.JPEG;
        }
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) {
            return ImageKind.PNG;
        }
        if (b0 == 'G' && b1 == 'I' && b2 == 'F' && b3 == '8') {
            return ImageKind.GIF;
        }
        if (b0 == 'R' && b1 == 'I' && b2 == 'F' && b3 == 'F'
            && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return ImageKind.WEBP;
        }
        return null;
    }

    static String sanitizeFilename(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isBlank()) {
            return "calendar-poster";
        }
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    public record PosterFile(String filename, String contentType, byte[] bytes) {
    }

    enum ImageKind {
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        GIF("image/gif", ".gif"),
        WEBP("image/webp", ".webp");

        private final String contentType;
        private final String extension;

        ImageKind(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        String contentType() {
            return contentType;
        }

        String extension() {
            return extension;
        }
    }
}
