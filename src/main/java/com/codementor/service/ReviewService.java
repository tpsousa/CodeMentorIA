package com.codementor.service;

import com.codementor.ai.AiAssistant;
import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.dto.ReviewRequest;
import com.codementor.dto.ReviewResponse;
import com.codementor.dto.ReviewResult;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final AiAssistant ai;
    private final AnalysisRecorder recorder;
    private final Resource reviewPrompt;

    public ReviewService(AiAssistant ai,
                         AnalysisRecorder recorder,
                         @Value("classpath:/prompts/review.st") Resource reviewPrompt) {
        this.ai = ai;
        this.recorder = recorder;
        this.reviewPrompt = reviewPrompt;
    }

    public ReviewResponse review(ReviewRequest request) {
        ReviewResult result = ai.ask(
                reviewPrompt,
                Map.of("language", request.language(), "code", request.code()),
                ReviewResult.class);

        Analysis saved = recorder.save(AnalysisType.REVIEW, request.code(), result);
        return ReviewResponse.from(saved, result);
    }
}
