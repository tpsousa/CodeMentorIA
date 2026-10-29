package com.codementor.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Paginação estável para a API (evita serializar PageImpl diretamente). */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
