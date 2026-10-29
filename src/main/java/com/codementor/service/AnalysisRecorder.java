package com.codementor.service;

import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.repository.AnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Persiste cada análise no histórico (resultado serializado em JSON). */
@Component
public class AnalysisRecorder {

    private final AnalysisRepository repository;
    private final ObjectMapper mapper;

    public AnalysisRecorder(AnalysisRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Analysis save(AnalysisType type, String input, Object result) {
        try {
            String json = result instanceof String s ? s : mapper.writeValueAsString(result);
            return repository.save(Analysis.of(type, input, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar o resultado da análise", e);
        }
    }
}
