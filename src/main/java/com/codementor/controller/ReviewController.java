package com.codementor.controller;

import com.codementor.dto.ReviewRequest;
import com.codementor.dto.ReviewResponse;
import com.codementor.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Code Review", description = "Revisão de código com IA")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Analisa um trecho de código e devolve problemas, nota e versão melhorada")
    public ReviewResponse review(@Valid @RequestBody ReviewRequest request) {
        return service.review(request);
    }
}
