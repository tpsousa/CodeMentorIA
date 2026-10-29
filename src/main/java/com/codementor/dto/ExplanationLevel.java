package com.codementor.dto;

public enum ExplanationLevel {
    BEGINNER("iniciante, sem jargões, com analogias do dia a dia"),
    INTERMEDIATE("intermediário, citando conceitos e boas práticas"),
    ADVANCED("avançado, discutindo complexidade, trade-offs e detalhes de implementação");

    private final String description;

    ExplanationLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
