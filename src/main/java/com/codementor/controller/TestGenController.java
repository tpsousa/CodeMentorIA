package com.codementor.controller;

import com.codementor.dto.TestGenRequest;
import com.codementor.dto.TestGenResponse;
import com.codementor.service.TestGenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tests")
@Tag(name = "Geração de testes", description = "Gera testes JUnit 5 + Mockito")
public class TestGenController {

    private final TestGenService service;

    public TestGenController(TestGenService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Gera testes unitários JUnit 5 para uma classe Java")
    public TestGenResponse generate(@Valid @RequestBody TestGenRequest request) {
        return service.generate(request);
    }
}
