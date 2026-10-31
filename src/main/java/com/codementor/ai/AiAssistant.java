package com.codementor.ai;

import com.codementor.exception.AiUnavailableException;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Único ponto de contato com a IA. Os services nunca usam o ChatClient diretamente,
 * o que facilita trocar de provedor, tratar erros e mockar nos testes.
 */
@Component
public class AiAssistant {

    private static final String SYSTEM_PROMPT = """
            Você é o CodeMentor, um mentor de programação preciso, didático e objetivo.
            Responda sempre em português do Brasil.
            """;

    private final ChatClient chatClient;

    public AiAssistant(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    /** Envia o prompt e converte a resposta da IA (JSON) em um objeto Java. */
    public <T> T ask(Resource promptTemplate, Map<String, Object> variables, Class<T> type) {
        T result;
        try {
            result = chatClient.prompt()
                    .user(u -> u.text(promptTemplate).params(variables))
                    .call()
                    .entity(type);
        } catch (Exception e) {
            throw new AiUnavailableException(
                    "Não foi possível obter uma resposta válida do provedor de IA.", e);
        }
        if (result == null) {
            throw new AiUnavailableException("O provedor de IA retornou uma resposta vazia.");
        }
        return result;
    }

    /** Envia o prompt e devolve a resposta em texto livre. */
    public String askText(Resource promptTemplate, Map<String, Object> variables) {
        String content;
        try {
            content = chatClient.prompt()
                    .user(u -> u.text(promptTemplate).params(variables))
                    .call()
                    .content();
        } catch (Exception e) {
            throw new AiUnavailableException(
                    "Não foi possível obter uma resposta do provedor de IA.", e);
        }
        if (content == null || content.isBlank()) {
            throw new AiUnavailableException("O provedor de IA retornou uma resposta vazia.");
        }
        return content;
    }

    /**
     * Envia o prompt e devolve a resposta em pedaços (streaming).
     * Erros do provedor, inclusive no meio do stream, viram {@link AiUnavailableException}.
     */
    public Flux<String> streamText(Resource promptTemplate, Map<String, Object> variables) {
        try {
            return chatClient.prompt()
                    .user(u -> u.text(promptTemplate).params(variables))
                    .stream()
                    .content()
                    .onErrorMap(e -> e instanceof AiUnavailableException
                            ? e
                            : new AiUnavailableException(
                                    "Não foi possível obter uma resposta do provedor de IA.", e));
        } catch (Exception e) {
            return Flux.error(new AiUnavailableException(
                    "Não foi possível obter uma resposta do provedor de IA.", e));
        }
    }
}
