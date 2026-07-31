package com.example.backend.dto.grade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchGradeRequest(@NotEmpty @Valid List<CreateGradeRequest> grades) {
}
