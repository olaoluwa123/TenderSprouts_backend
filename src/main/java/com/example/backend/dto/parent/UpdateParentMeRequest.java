package com.example.backend.dto.parent;

import jakarta.validation.constraints.Size;

public record UpdateParentMeRequest(
    @Size(max = 150) String fullName,
    @Size(max = 30) String phone
) {
}
