package com.codementor.dto;

import com.codementor.domain.Analysis;
import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        Long id,
        int score,
        String summary,
        List<ReviewResult.Issue> issues,
        String improvedCode,
        Instant createdAt) {

    public static ReviewResponse from(Analysis saved, ReviewResult result) {
        int score = Math.max(0, Math.min(10, result.score()));
        List<ReviewResult.Issue> issues = result.issues() == null ? List.of() : result.issues();
        return new ReviewResponse(saved.getId(), score, result.summary(), issues,
                result.improvedCode(), saved.getCreatedAt());
    }
}
