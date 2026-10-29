package com.codementor.dto;

import com.codementor.domain.Analysis;
import java.time.Instant;
import java.util.List;

public record TestGenResponse(
        Long id,
        String testClassName,
        String testCode,
        List<String> scenariosCovered,
        Instant createdAt) {

    public static TestGenResponse from(Analysis saved, TestGenResult result) {
        List<String> scenarios = result.scenariosCovered() == null ? List.of() : result.scenariosCovered();
        return new TestGenResponse(saved.getId(), result.testClassName(), result.testCode(),
                scenarios, saved.getCreatedAt());
    }
}
