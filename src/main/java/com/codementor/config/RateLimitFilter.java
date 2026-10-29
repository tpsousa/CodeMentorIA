package com.codementor.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite simples de requisições por IP (janela fixa de 1 minuto, em memória).
 * Protege a cota do plano gratuito do provedor de IA.
 * Para múltiplas instâncias, troque por uma solução distribuída (ex.: Redis).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private record Window(long start, AtomicInteger count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limitPerMinute;

    public RateLimitFilter(@Value("${app.rate-limit.requests-per-minute:10}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.clear();
        }

        Window window = windows.compute(request.getRemoteAddr(), (ip, old) -> {
            if (old == null || now - old.start() >= WINDOW_MS) {
                return new Window(now, new AtomicInteger(1));
            }
            old.count().incrementAndGet();
            return old;
        });

        if (window.count().get() > limitPerMinute) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                            + "\"detail\":\"Limite de requisições excedido. Tente novamente em instantes.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
