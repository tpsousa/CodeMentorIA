package com.codementor.controller;

import com.codementor.domain.AnalysisType;
import com.codementor.dto.HistoryItem;
import com.codementor.dto.PageResponse;
import com.codementor.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@Tag(name = "Histórico", description = "Consulta de análises anteriores")
public class HistoryController {

    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista o histórico paginado (mais recentes primeiro)")
    public PageResponse<HistoryItem> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AnalysisType type) {
        return service.list(type, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma análise do histórico")
    public HistoryItem get(@PathVariable Long id) {
        return service.findById(id);
    }
}
