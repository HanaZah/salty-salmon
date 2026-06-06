package com.finadvise.crm.documents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentUpdateRequestDTO(
        @NotBlank(message = "File name cannot be blank")
        @Size(max = 255, message = "File name cannot exceed 255 characters")
        String fileName,

        Long productId
) {}
