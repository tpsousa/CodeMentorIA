package com.codementor.controller;

import com.codementor.dto.ExplanationRequest;
import com.codementor.dto.ExplanationResponse;
import com.codementor.service.ExplanationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/explanations")
@Tag(name = "Explicação de código", description = "Explica código em diferentes níveis")
public class ExplanationController {

    private final ExplanationService service;

    public ExplanationController(ExplanationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Explica um trecho de código no nível escolhido")
    public ExplanationResponse explain(@Valid @RequestBody ExplanationRequest request) {
        return service.explain(request);
    }
}
