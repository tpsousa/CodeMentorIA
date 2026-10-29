package com.codementor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplanationRequest(
        @NotBlank(message = "A linguagem é obrigatória")
        @Size(max = 30, message = "Linguagem deve ter no máximo 30 caracteres")
        String language,

        @NotBlank(message = "O código não pode ser vazio")
        @Size(max = 8000, message = "O código excede o limite de 8000 caracteres")
        String code,

        ExplanationLevel level) {

    public ExplanationRequest {
        if (level == null) {
            level = ExplanationLevel.BEGINNER;
        }
    }
}
