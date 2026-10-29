package com.codementor.service;

import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.dto.HistoryItem;
import com.codementor.dto.PageResponse;
import com.codementor.exception.NotFoundException;
import com.codementor.repository.AnalysisRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final AnalysisRepository repository;

    public HistoryService(AnalysisRepository repository) {
        this.repository = repository;
    }

    public PageResponse<HistoryItem> list(AnalysisType type, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Analysis> result = (type == null)
                ? repository.findAll(pageable)
                : repository.findByType(type, pageable);

        return PageResponse.from(result.map(HistoryItem::from));
    }

    public HistoryItem findById(Long id) {
        return repository.findById(id)
                .map(HistoryItem::from)
                .orElseThrow(() -> new NotFoundException("Análise " + id + " não encontrada"));
    }
}
