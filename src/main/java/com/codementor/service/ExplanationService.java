package com.codementor.service;

import com.codementor.ai.AiAssistant;
import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.dto.ExplanationRequest;
import com.codementor.dto.ExplanationResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ExplanationService {

    private final AiAssistant ai;
    private final AnalysisRecorder recorder;
    private final Resource explainPrompt;

    public ExplanationService(AiAssistant ai,
                              AnalysisRecorder recorder,
                              @Value("classpath:/prompts/explain.st") Resource explainPrompt) {
        this.ai = ai;
        this.recorder = recorder;
        this.explainPrompt = explainPrompt;
    }

    public ExplanationResponse explain(ExplanationRequest request) {
        String explanation = ai.askText(
                explainPrompt,
                Map.of("language", request.language(),
                       "code", request.code(),
                       "level", request.level().getDescription()));

        Analysis saved = recorder.save(AnalysisType.EXPLANATION, request.code(), explanation);
        return ExplanationResponse.from(saved, request.level(), explanation);
    }
}
