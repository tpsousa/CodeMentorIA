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
import com.codementor.dto.TestGenRequest;
import com.codementor.dto.TestGenResponse;
import com.codementor.dto.TestGenResult;
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

@ExtendWith(MockitoExtension.class)
class TestGenServiceTest {

    @Mock AiAssistant ai;
    @Mock AnalysisRecorder recorder;
    @Captor ArgumentCaptor<Map<String, Object>> variablesCaptor;

    TestGenService service;

    @BeforeEach
    void setUp() {
        service = new TestGenService(ai, recorder, new ClassPathResource("prompts/generate-tests.st"));
    }

    @Test
    void deveGerarTestesEPersistirNoHistorico() {
        var fake = new TestGenResult("CalculadoraTest",
                "class CalculadoraTest { }", List.of("soma de positivos", "soma com zero"));
        var saved = Analysis.of(AnalysisType.TESTS, "class Calculadora { }", "{}");

        when(ai.ask(any(), anyMap(), eq(TestGenResult.class))).thenReturn(fake);
        when(recorder.save(eq(AnalysisType.TESTS), eq("class Calculadora { }"), eq(fake))).thenReturn(saved);

        TestGenResponse response = service.generate(new TestGenRequest("Calculadora", "class Calculadora { }"));

        assertThat(response.testClassName()).isEqualTo("CalculadoraTest");
        assertThat(response.testCode()).isEqualTo("class CalculadoraTest { }");
        assertThat(response.scenariosCovered()).containsExactly("soma de positivos", "soma com zero");
        assertThat(response.createdAt()).isEqualTo(saved.getCreatedAt());
        verify(recorder).save(AnalysisType.TESTS, "class Calculadora { }", fake);
    }

    @Test
    void deveEnviarNomeDaClasseECodigoParaAI() {
        var fake = new TestGenResult("CalculadoraTest", "class CalculadoraTest { }", List.of());
        var saved = Analysis.of(AnalysisType.TESTS, "int somar(int a, int b) { return a + b; }", "{}");

        when(ai.ask(any(), anyMap(), eq(TestGenResult.class))).thenReturn(fake);
        when(recorder.save(any(), any(), any())).thenReturn(saved);

        service.generate(new TestGenRequest("Calculadora", "int somar(int a, int b) { return a + b; }"));

        verify(ai).ask(any(), variablesCaptor.capture(), eq(TestGenResult.class));
        assertThat(variablesCaptor.getValue())
                .containsEntry("className", "Calculadora")
                .containsEntry("code", "int somar(int a, int b) { return a + b; }");
    }

    @Test
    void deveUsarListaVaziaQuandoIaNaoInformaCenarios() {
        var fake = new TestGenResult("CalculadoraTest", "class CalculadoraTest { }", null);
        var saved = Analysis.of(AnalysisType.TESTS, "x", "{}");

        when(ai.ask(any(), anyMap(), eq(TestGenResult.class))).thenReturn(fake);
        when(recorder.save(any(), any(), any())).thenReturn(saved);

        TestGenResponse response = service.generate(new TestGenRequest("Calculadora", "x"));

        assertThat(response.scenariosCovered()).isEmpty();
    }
}
