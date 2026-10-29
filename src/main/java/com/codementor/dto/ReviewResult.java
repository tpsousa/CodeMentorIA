package com.codementor.dto;

import java.util.List;

/** Estrutura que a IA deve devolver (convertida automaticamente do JSON). */
public record ReviewResult(
        int score,
        String summary,
        List<Issue> issues,
        String improvedCode) {

    public record Issue(Severity severity, Integer line, String description, String suggestion) {
    }

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
}
