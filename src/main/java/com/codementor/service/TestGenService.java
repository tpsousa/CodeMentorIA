package com.codementor.service;

import com.codementor.ai.AiAssistant;
import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.dto.TestGenRequest;
import com.codementor.dto.TestGenResponse;
import com.codementor.dto.TestGenResult;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class TestGenService {

    private final AiAssistant ai;
    private final AnalysisRecorder recorder;
    private final Resource testsPrompt;

    public TestGenService(AiAssistant ai,
                          AnalysisRecorder recorder,
                          @Value("classpath:/prompts/generate-tests.st") Resource testsPrompt) {
        this.ai = ai;
        this.recorder = recorder;
        this.testsPrompt = testsPrompt;
    }

    public TestGenResponse generate(TestGenRequest request) {
        TestGenResult result = ai.ask(
                testsPrompt,
                Map.of("className", request.className(), "code", request.code()),
                TestGenResult.class);

        Analysis saved = recorder.save(AnalysisType.TESTS, request.code(), result);
        return TestGenResponse.from(saved, result);
    }
}
