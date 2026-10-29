package com.codementor.dto;

import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import java.time.Instant;

public record HistoryItem(
        Long id,
        AnalysisType type,
        String inputCode,
        String resultJson,
        Instant createdAt) {

    public static HistoryItem from(Analysis a) {
        return new HistoryItem(a.getId(), a.getType(), a.getInputCode(), a.getResultJson(), a.getCreatedAt());
    }
}
