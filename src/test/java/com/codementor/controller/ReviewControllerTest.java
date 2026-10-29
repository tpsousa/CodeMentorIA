package com.codementor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codementor.dto.ReviewResponse;
import com.codementor.exception.AiUnavailableException;
import com.codementor.service.ReviewService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ReviewService service;

    @Test
    void deveRetornar200ComARevisao() throws Exception {
        when(service.review(any())).thenReturn(
                new ReviewResponse(1L, 7, "Bom código", List.of(), "melhorado", Instant.now()));

        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"java\",\"code\":\"int x = 1;\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(7))
                .andExpect(jsonPath("$.summary").value("Bom código"));
    }

    @Test
    void deveRetornar400QuandoCodigoVazio() throws Exception {
        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"java\",\"code\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar502QuandoIaIndisponivel() throws Exception {
        when(service.review(any())).thenThrow(new AiUnavailableException("IA fora do ar"));

        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"java\",\"code\":\"int x = 1;\"}"))
                .andExpect(status().isBadGateway());
    }
}
