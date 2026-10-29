# CodeMentor AI — Guia para o Claude Code

## Comandos
- Rodar: `mvn spring-boot:run`
- Testes: `mvn test`
- Build: `mvn -DskipTests package`

## Arquitetura
controller → service → (AiAssistant | AnalysisRecorder → repository)

## Convenções
- Java 21; use `record` para DTOs.
- Controllers não acessam repositórios; só services.
- Chamadas à IA SOMENTE via `AiAssistant`. Nunca use o `ChatClient` direto nos services.
- Prompts ficam em `src/main/resources/prompts/*.st`. Não use chaves `{}` literais nos prompts
  (são variáveis do template); descreva o JSON em texto.
- Todo novo endpoint precisa de: validação, teste unitário e anotações OpenAPI.
- Testes NUNCA chamam a IA real; use Mockito.
- Mensagens ao usuário em português do Brasil.

## Não fazer
- Não commitar chaves de API; use variáveis de ambiente (`.env` está no .gitignore).
- Não adicionar outro starter de modelo (Ollama/Gemini/Groq já funcionam via API compatível com OpenAI).
