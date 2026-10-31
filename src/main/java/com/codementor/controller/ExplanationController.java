package com.codementor.controller;

import com.codementor.dto.ExplanationRequest;
import com.codementor.dto.ExplanationResponse;
import com.codementor.dto.ExplanationStreamEvent;
import com.codementor.exception.AiUnavailableException;
import com.codementor.service.ExplanationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/explanations")
@Tag(name = "Explicação de código", description = "Explica código em diferentes níveis")
public class ExplanationController {

    private static final Logger log = LoggerFactory.getLogger(ExplanationController.class);

    private final ExplanationService service;
    private final ObjectMapper mapper;

    public ExplanationController(ExplanationService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Explica um trecho de código no nível escolhido")
    public ExplanationResponse explain(@Valid @RequestBody ExplanationRequest request) {
        return service.explain(request);
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Explica um trecho de código em streaming (SSE)",
            description = "Emite eventos 'token' com pedaços do texto, 'done' com o id salvo no "
                    + "histórico e 'error' se o provedor falhar no meio do stream.")
    @ApiResponse(responseCode = "200", description = "Stream de eventos SSE",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE))
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ExplanationRequest request) {
        return service.stream(request)
                .map(this::toEvent)
                .onErrorResume(AiUnavailableException.class, this::errorEvent);
    }

    private ServerSentEvent<String> toEvent(ExplanationStreamEvent event) {
        if (event instanceof ExplanationStreamEvent.Token token) {
            return ServerSentEvent.<String>builder()
                    .event("token")
                    .data(token.text())
                    .build();
        }
        var done = (ExplanationStreamEvent.Done) event;
        return ServerSentEvent.<String>builder()
                .event("done")
                .data(json(done))
                .build();
    }

    private Flux<ServerSentEvent<String>> errorEvent(AiUnavailableException ex) {
        log.error("Falha no provedor de IA durante o streaming", ex);
        String detail = ex.getMessage() == null ? "Falha no provedor de IA." : ex.getMessage();
        return Flux.just(ServerSentEvent.<String>builder()
                .event("error")
                .data(json(Map.of("detail", detail)))
                .build());
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar evento SSE", e);
        }
    }
}
