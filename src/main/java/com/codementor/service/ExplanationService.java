package com.codementor.service;

import com.codementor.ai.AiAssistant;
import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.dto.ExplanationRequest;
import com.codementor.dto.ExplanationResponse;
import com.codementor.dto.ExplanationStreamEvent;
import com.codementor.exception.AiUnavailableException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
        String explanation = ai.askText(explainPrompt, variables(request));

        Analysis saved = recorder.save(AnalysisType.EXPLANATION, request.code(), explanation);
        return ExplanationResponse.from(saved, request.level(), explanation);
    }

    /**
     * Explica em streaming: cada pedaço da IA vira um {@code Token} e, ao final,
     * a explicação completa é salva no histórico e um {@code Done} é emitido.
     * Se a IA falhar, o histórico não é gravado.
     */
    public Flux<ExplanationStreamEvent> stream(ExplanationRequest request) {
        return Flux.defer(() -> {
            StringBuilder buffer = new StringBuilder();

            return ai.streamText(explainPrompt, variables(request))
                    .doOnNext(buffer::append)
                    .<ExplanationStreamEvent>map(ExplanationStreamEvent.Token::new)
                    .concatWith(Flux.<ExplanationStreamEvent>defer(() -> {
                        String explanation = buffer.toString();
                        if (explanation.isBlank()) {
                            return Flux.error(new AiUnavailableException(
                                    "O provedor de IA retornou uma resposta vazia."));
                        }
                        Analysis saved = recorder.save(AnalysisType.EXPLANATION, request.code(), explanation);
                        return Flux.just(new ExplanationStreamEvent.Done(
                                saved.getId(), request.level(), saved.getCreatedAt()));
                    }));
        });
    }

    private Map<String, Object> variables(ExplanationRequest request) {
        return Map.of("language", request.language(),
                      "code", request.code(),
                      "level", request.level().getDescription());
    }
}
