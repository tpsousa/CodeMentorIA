package com.codementor.dto;

import com.codementor.domain.Analysis;
import java.time.Instant;

public record ExplanationResponse(
        Long id,
        ExplanationLevel level,
        String explanation,
        Instant createdAt) {

    public static ExplanationResponse from(Analysis saved, ExplanationLevel level, String explanation) {
        return new ExplanationResponse(saved.getId(), level, explanation, saved.getCreatedAt());
    }
}
