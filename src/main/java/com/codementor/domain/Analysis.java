package com.codementor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Registro do histórico de uma análise feita pela IA. */
@Entity
@Table(name = "analysis")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisType type;

    @Column(name = "input_code", nullable = false, columnDefinition = "TEXT")
    private String inputCode;

    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Analysis() {
        // exigido pelo JPA
    }

    public static Analysis of(AnalysisType type, String inputCode, String resultJson) {
        Analysis a = new Analysis();
        a.type = type;
        a.inputCode = inputCode;
        a.resultJson = resultJson;
        return a;
    }

    public Long getId() { return id; }
    public AnalysisType getType() { return type; }
    public String getInputCode() { return inputCode; }
    public String getResultJson() { return resultJson; }
    public Instant getCreatedAt() { return createdAt; }
}
