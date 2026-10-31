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
import com.codementor.dto.ExplanationLevel;
import com.codementor.dto.ExplanationRequest;
import com.codementor.dto.ExplanationResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class ExplanationServiceTest {

    @Mock AiAssistant ai;
    @Mock AnalysisRecorder recorder;
    @Captor ArgumentCaptor<Map<String, Object>> variablesCaptor;

    ExplanationService service;

    @BeforeEach
    void setUp() {
        service = new ExplanationService(ai, recorder, new ClassPathResource("prompts/explain.st"));
    }

    @Test
    void deveExplicarCodigoEPersistirNoHistorico() {
        var saved = Analysis.of(AnalysisType.EXPLANATION, "a + b", "{}");

        when(ai.askText(any(), anyMap())).thenReturn("## Explicação\nSoma dois números.");
        when(recorder.save(eq(AnalysisType.EXPLANATION), eq("a + b"), eq("## Explicação\nSoma dois números.")))
                .thenReturn(saved);

        ExplanationResponse response =
                service.explain(new ExplanationRequest("java", "a + b", ExplanationLevel.INTERMEDIATE));

        assertThat(response.level()).isEqualTo(ExplanationLevel.INTERMEDIATE);
        assertThat(response.explanation()).isEqualTo("## Explicação\nSoma dois números.");
        assertThat(response.createdAt()).isEqualTo(saved.getCreatedAt());
        verify(recorder).save(AnalysisType.EXPLANATION, "a + b", "## Explicação\nSoma dois números.");
    }

    @Test
    void deveAssumirNivelInicianteQuandoNaoInformado() {
        var saved = Analysis.of(AnalysisType.EXPLANATION, "a + b", "{}");

        when(ai.askText(any(), anyMap())).thenReturn("explicação simples");
        when(recorder.save(any(), any(), any())).thenReturn(saved);

        ExplanationResponse response = service.explain(new ExplanationRequest("java", "a + b", null));

        assertThat(response.level()).isEqualTo(ExplanationLevel.BEGINNER);
        verify(ai).askText(any(), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue())
                .containsEntry("language", "java")
                .containsEntry("code", "a + b")
                .containsEntry("level", ExplanationLevel.BEGINNER.getDescription());
    }
}
