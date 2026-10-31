package com.codementor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import com.codementor.dto.HistoryItem;
import com.codementor.dto.PageResponse;
import com.codementor.exception.NotFoundException;
import com.codementor.repository.AnalysisRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock AnalysisRepository repository;
    @Captor ArgumentCaptor<Pageable> pageableCaptor;

    HistoryService service;

    @BeforeEach
    void setUp() {
        service = new HistoryService(repository);
    }

    @Test
    void deveListarFiltrandoPorTipo() {
        var analysis = Analysis.of(AnalysisType.REVIEW, "a / b", "{}");
        Page<Analysis> page = new PageImpl<>(List.of(analysis), PageRequest.of(0, 10), 1);

        when(repository.findByType(eq(AnalysisType.REVIEW), any(Pageable.class))).thenReturn(page);

        PageResponse<HistoryItem> response = service.list(AnalysisType.REVIEW, 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).type()).isEqualTo(AnalysisType.REVIEW);
        assertThat(response.content().get(0).inputCode()).isEqualTo("a / b");
        assertThat(response.totalElements()).isEqualTo(1);
        verify(repository).findByType(eq(AnalysisType.REVIEW), any(Pageable.class));
    }

    @Test
    void deveListarTudoQuandoTipoNulo() {
        var analysis = Analysis.of(AnalysisType.TESTS, "class A { }", "{}");
        Page<Analysis> page = new PageImpl<>(List.of(analysis), PageRequest.of(0, 10), 1);

        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<HistoryItem> response = service.list(null, 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).type()).isEqualTo(AnalysisType.TESTS);
        verify(repository).findAll(any(Pageable.class));
    }

    @Test
    void deveClampearPaginaETamanhoEOrdenarPorDataDecrescente() {
        Page<Analysis> page = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);

        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        service.list(null, -3, 999);

        verify(repository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void deveUsarTamanhoMinimoDeUm() {
        Page<Analysis> page = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);

        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        service.list(null, 0, 0);

        verify(repository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void deveBuscarAnalisePorId() {
        var analysis = Analysis.of(AnalysisType.EXPLANATION, "a + b", "explicação");

        when(repository.findById(1L)).thenReturn(Optional.of(analysis));

        HistoryItem item = service.findById(1L);

        assertThat(item.type()).isEqualTo(AnalysisType.EXPLANATION);
        assertThat(item.inputCode()).isEqualTo("a + b");
        assertThat(item.resultJson()).isEqualTo("explicação");
    }

    @Test
    void deveLancarNotFoundQuandoAnaliseNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }
}
