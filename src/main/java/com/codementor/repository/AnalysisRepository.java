package com.codementor.repository;

import com.codementor.domain.Analysis;
import com.codementor.domain.AnalysisType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Page<Analysis> findByType(AnalysisType type, Pageable pageable);
}
