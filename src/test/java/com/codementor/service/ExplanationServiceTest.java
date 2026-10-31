package com.codementor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codementor.ai.AiAssistant;
import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.dto.ExplanationLevel;
import com.codementor.dto.ExplanationRequest;
import com.codementor.dto.ExplanationResponse;
import com.codementor.dto.ExplanationStreamEvent;
import com.codementor.exception.AiUnavailableException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

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

    @Test
    void deveFazerStreamingEPersistirAoFinal() {
        var saved = Analysis.of(AnalysisType.EXPLANATION, "a + b", "{}");

        when(ai.streamText(any(), anyMap())).thenReturn(Flux.just("Olá", " mundo"));
        when(recorder.save(eq(AnalysisType.EXPLANATION), eq("a + b"), eq("Olá mundo"))).thenReturn(saved);

        List<ExplanationStreamEvent> events = service
                .stream(new ExplanationRequest("java", "a + b", ExplanationLevel.ADVANCED))
                .collectList()
                .block();

        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isEqualTo(new ExplanationStreamEvent.Token("Olá"));
        assertThat(events.get(1)).isEqualTo(new ExplanationStreamEvent.Token(" mundo"));

        assertThat(events.get(2)).isInstanceOf(ExplanationStreamEvent.Done.class);
        var done = (ExplanationStreamEvent.Done) events.get(2);
        assertThat(done.level()).isEqualTo(ExplanationLevel.ADVANCED);
        assertThat(done.createdAt()).isEqualTo(saved.getCreatedAt());

        verify(ai).streamText(any(), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue())
                .containsEntry("level", ExplanationLevel.ADVANCED.getDescription());
        verify(recorder).save(AnalysisType.EXPLANATION, "a + b", "Olá mundo");
    }

    @Test
    void naoDevePersistirQuandoStreamingFalha() {
        when(ai.streamText(any(), anyMap()))
                .thenReturn(Flux.error(new AiUnavailableException("IA fora do ar")));

        assertThatThrownBy(() -> service
                .stream(new ExplanationRequest("java", "a + b", null))
                .blockLast())
                .isInstanceOf(AiUnavailableException.class)
                .hasMessage("IA fora do ar");

        verify(recorder, never()).save(any(), any(), any());
    }

    @Test
    void deveFalharQuandoStreamingNaoRetornaConteudo() {
        when(ai.streamText(any(), anyMap())).thenReturn(Flux.empty());

        assertThatThrownBy(() -> service
                .stream(new ExplanationRequest("java", "a + b", null))
                .blockLast())
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("resposta vazia");

        verify(recorder, never()).save(any(), any(), any());
    }
}
