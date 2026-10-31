package com.codementor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codementor.dto.ExplanationLevel;
import com.codementor.dto.ExplanationStreamEvent;
import com.codementor.exception.AiUnavailableException;
import com.codementor.service.ExplanationService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

@WebMvcTest(ExplanationController.class)
class ExplanationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ExplanationService service;

    @Test
    void deveTransmitirTokensEDoneEmSse() throws Exception {
        when(service.stream(any())).thenReturn(Flux.just(
                new ExplanationStreamEvent.Token("Olá"),
                new ExplanationStreamEvent.Token(" mundo"),
                new ExplanationStreamEvent.Done(1L, ExplanationLevel.BEGINNER,
                        Instant.parse("2026-01-15T14:30:00Z"))));

        MvcResult result = mockMvc.perform(post("/api/v1/explanations/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"java\",\"code\":\"a + b\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatched = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        // SSE é sempre UTF-8 na rede; o MockMvc não define charset na resposta.
        String body = dispatched.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body)
                .contains("event:token")
                .contains("data:Olá")
                .contains("data: mundo")
                .contains("event:done")
                .contains("\"id\":1")
                .contains("\"level\":\"BEGINNER\"");
    }

    @Test
    void deveEmitirEventoDeErroQuandoIaFalha() throws Exception {
        when(service.stream(any()))
                .thenReturn(Flux.error(new AiUnavailableException("IA fora do ar")));

        MvcResult result = mockMvc.perform(post("/api/v1/explanations/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"java\",\"code\":\"a + b\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:error")))
                .andExpect(content().string(containsString("IA fora do ar")));
    }

    @Test
    void deveRetornar400QuandoCodigoVazio() throws Exception {
        mockMvc.perform(post("/api/v1/explanations/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"java\",\"code\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
