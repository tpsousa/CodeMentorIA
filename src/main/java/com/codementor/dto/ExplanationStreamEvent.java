package com.codementor.dto;

import java.time.Instant;

/** Eventos emitidos pelo endpoint de explicação em streaming (SSE). */
public sealed interface ExplanationStreamEvent {

    /** Pedaço de texto gerado pela IA. */
    record Token(String text) implements ExplanationStreamEvent {
    }

    /** Fim do stream; a análise já foi salva no histórico. */
    record Done(Long id, ExplanationLevel level, Instant createdAt) implements ExplanationStreamEvent {
    }
}
