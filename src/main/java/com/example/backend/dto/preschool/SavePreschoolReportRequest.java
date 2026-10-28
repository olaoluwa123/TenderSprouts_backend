package com.example.backend.dto.preschool;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SavePreschoolReportRequest(
    @NotNull Long classId,
    @NotNull Long termId,
    @Min(0) @Max(4) Integer monthNumber,
    @NotEmpty @Valid List<Pupil> pupils
) {
    public record Pupil(
        @NotNull Long studentId,
        @Valid List<Comment> comments,
        @Size(max = 4000) String classTeacherComment,
        @Size(max = 4000) String headTeacherRemark
    ) {
    }

    public record Comment(
        @NotNull Long subjectId,
        @Size(max = 4000) String comment
    ) {
    }
}
