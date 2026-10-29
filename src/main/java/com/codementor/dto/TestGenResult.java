package com.codementor.dto;

import java.util.List;

/** Estrutura que a IA deve devolver ao gerar testes. */
public record TestGenResult(
        String testClassName,
        String testCode,
        List<String> scenariosCovered) {
}
