package com.codementor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codementor.ai.AiAssistant;
import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.dto.ReviewRequest;
import com.codementor.dto.ReviewResponse;
import com.codementor.dto.ReviewResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock AiAssistant ai;
    @Mock AnalysisRecorder recorder;

    ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(ai, recorder, new ClassPathResource("prompts/review.st"));
    }

    @Test
    void deveRetornarRevisaoEPersistirNoHistorico() {
        var issue = new ReviewResult.Issue(ReviewResult.Severity.HIGH, 1,
                "Divisão por zero", "Valide o divisor");
        var fake = new ReviewResult(5, "Falta tratar divisão por zero", List.of(issue), "codigo melhorado");
        var saved = Analysis.of(AnalysisType.REVIEW, "a / b", "{}");

        when(ai.ask(any(), anyMap(), eq(ReviewResult.class))).thenReturn(fake);
        when(recorder.save(eq(AnalysisType.REVIEW), eq("a / b"), eq(fake))).thenReturn(saved);

        ReviewResponse response = service.review(new ReviewRequest("java", "a / b"));

        assertThat(response.score()).isEqualTo(5);
        assertThat(response.issues()).hasSize(1);
        verify(recorder).save(AnalysisType.REVIEW, "a / b", fake);
    }

    @Test
    void deveLimitarANotaEntreZeroEDez() {
        var fake = new ReviewResult(42, "nota absurda", null, "x");
        var saved = Analysis.of(AnalysisType.REVIEW, "x", "{}");

        when(ai.ask(any(), anyMap(), eq(ReviewResult.class))).thenReturn(fake);
        when(recorder.save(any(), any(), any())).thenReturn(saved);

        ReviewResponse response = service.review(new ReviewRequest("java", "x"));

        assertThat(response.score()).isEqualTo(10);
        assertThat(response.issues()).isEmpty();
    }
}
