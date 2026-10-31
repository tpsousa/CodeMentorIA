# CodeMentor AI

[English](README.md) | **Português (BR)**

> API REST em **Java 21 + Spring Boot 3.5 + Spring AI** que atua como um **mentor de código com IA**: faz *code review*, gera **testes unitários JUnit 5**, **explica código** em três níveis (inclusive em streaming) e guarda o histórico de tudo.
>
> **Gratuito de rodar**: usa **Groq** (free tier) por padrão e troca para **Ollama (modelos locais, offline)**, **Google Gemini** ou **OpenAI** só com variáveis de ambiente, sem mudar uma linha de código. O desenvolvimento do projeto é feito com o agente open source **OpenCode**.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-6DB33F)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker)# CodeMentor AI

**English** | [Português (BR)](README.pt-BR.md)

> REST API built with **Java 21 + Spring Boot 3.5 + Spring AI** that works as an **AI code mentor**: it performs *code reviews*, generates **JUnit 5 unit tests**, **explains code** at three levels (including streaming) and keeps a history of every analysis.
>
> **Free to run**: it uses **Groq** (free tier) by default and switches to **Ollama (local models, offline)**, **Google Gemini** or **OpenAI** through environment variables only, without changing a single line of code. The project itself is developed with the open source agent **OpenCode**.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-6DB33F)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker)
![License](https://img.shields.io/badge/license-MIT-blue)

---

## Table of Contents

1. [About the project](#about-the-project)
2. [Why these choices](#why-these-choices)
3. [Features](#features)
4. [Architecture](#architecture)
5. [AI models](#ai-models)
6. [Getting started](#getting-started)
7. [Configuration](#configuration)
8. [API endpoints](#api-endpoints)
9. [Project structure](#project-structure)
10. [How the AI is integrated](#how-the-ai-is-integrated)
11. [Tests](#tests)
12. [Docker](#docker)
13. [Developing with OpenCode and other AI agents](#developing-with-opencode-and-other-ai-agents)
14. [Security](#security)
15. [Troubleshooting](#troubleshooting)
16. [Roadmap](#roadmap)
17. [Contributing](#contributing)
18. [License](#license)

---

## About the project

A portfolio project showing how to integrate **LLMs** into a Java application in a clean, professional way:

- AI integration through **Spring AI** (`ChatClient`), with no vendor-specific SDK.
- **Structured outputs**: the AI answers in JSON, automatically converted into Java `records`.
- **Versioned prompts** stored in `.st` files, separate from the code.
- **Interchangeable provider** (Groq, Gemini, Ollama, OpenAI) through configuration.
- **Streaming (SSE)** for explanations, with persistence once the stream finishes.
- Persistence (JPA + Flyway), validation, RFC 7807 error responses, OpenAPI/Swagger, rate limiting, tests, Docker and CI.

**Problem it solves:** junior developers and small teams do not always have someone to review their code or write tests. CodeMentor AI is a reviewer available 24/7 that explains **what is wrong, why, and how to fix it**.

> **Note:** by default the AI answers in **Brazilian Portuguese**. This is defined in the system prompt (`AiAssistant`) and in the files under `src/main/resources/prompts/`. Edit them to change the output language.

---

## Why these choices

| Decision | Reason |
|---|---|
| **Groq as the default API provider** | Free tier with no credit card, very fast, the `llama-3.3-70b-versatile` model produces reliable JSON, and the API is OpenAI-compatible |
| **Ollama as the local alternative** | Runs offline, has no usage limit and does not send your code to the cloud |
| **`qwen2.5-coder:7b` as the recommended local model** | Code-focused and fits laptops with 16 GB of RAM |
| **Gemini as an extra option** | Free tier whose limits vary; check the current documentation |
| **Spring AI instead of a provider SDK** | One abstraction; switching providers means changing environment variables |
| **A single starter (`spring-ai-starter-model-openai`)** | Groq, Gemini and Ollama expose an OpenAI-compatible API |
| **`AiAssistant` as the single entry point** | Isolates the AI, centralizes error handling and makes mocking in tests easy |
| **H2 in dev, PostgreSQL in prod** | Runs with nothing to install, yet is production-ready |
| **Custom rate limiter (no extra dependency)** | Protects the free quota and is simple to understand |
| **OpenCode as the development agent** | Open source (MIT), supports many providers and local models, with no single-vendor lock-in |

---

## Features

| Feature | Description |
|---|---|
| **Code Review** | Returns a 0–10 score, issues with severity, suggestions and improved code |
| **Test generator** | Generates JUnit 5 + Mockito tests for a Java class |
| **Explainer** | Explains code at `BEGINNER`, `INTERMEDIATE` or `ADVANCED` level |
| **Streaming (SSE)** | Explanations arrive in chunks through `text/event-stream` |
| **History** | Every analysis is saved, with pagination and filtering by type |
| **Multi-provider** | Groq, Gemini, Ollama or OpenAI through environment variables |
| **Swagger UI** | Interactive documentation at `/swagger-ui.html` |
| **Rate limiting** | Per-IP, per-minute request limit to protect your quota |

---

## Architecture

```
Client ──▶ Controller ──▶ Service ──▶ AiAssistant ──▶ Spring AI ChatClient ──▶ Groq / Gemini / Ollama
 (REST)    (validation,   (rules)      (single AI         │
            DTOs)             │         entry point)      └─ prompts/*.st
                              ▼
                       AnalysisRecorder ──▶ Repository (JPA) ──▶ H2 (dev) / PostgreSQL (prod)
```

Code review flow:

1. `POST /api/v1/reviews` reaches `ReviewController`, which validates the JSON (`@Valid`).
2. `ReviewService` builds the variables and calls `AiAssistant.ask(...)` with the `review.st` prompt.
3. Spring AI sends the prompt to the provider and converts the JSON answer into a `ReviewResult`.
4. `AnalysisRecorder` saves it to the history and the service returns a `ReviewResponse` (score clamped to 0–10).
5. Any AI failure becomes a `502 Bad Gateway` through `GlobalExceptionHandler`.

---

## AI models

The project uses AI in **two different roles**, which should not be confused:

1. **API engine:** the model CodeMentor calls at runtime to review, test and explain code (configured through environment variables).
2. **Development agent:** the terminal tool used to write and evolve the project code (OpenCode, described [below](#developing-with-opencode-and-other-ai-agents)).

### 1. API engine (configurable providers)

> **Warning:** free-tier limits and model names change frequently. Check each provider's official documentation.

| Provider | Cost | Key | `AI_BASE_URL` | `AI_COMPLETIONS_PATH` | Example `AI_MODEL` |
|---|---|---|---|---|---|
| **Groq** (default) | Free tier | [console.groq.com](https://console.groq.com) | `https://api.groq.com/openai` | `/v1/chat/completions` | `llama-3.3-70b-versatile` |
| **Ollama** (local) | Free, unlimited | not needed (use `ollama`) | `http://localhost:11434` | `/v1/chat/completions` | `qwen2.5-coder:7b` |
| **Google Gemini** | Free tier (check current limits) | [aistudio.google.com](https://aistudio.google.com) | `https://generativelanguage.googleapis.com/v1beta/openai` | `/chat/completions` | see the current model list |
| **OpenAI** | Paid | [platform.openai.com](https://platform.openai.com) | `https://api.openai.com` | `/v1/chat/completions` | `gpt-4o-mini` |

**Local models tested in this project (Ollama):**

| Model | Approx. size | Recommended use |
|---|---|---|
| `qwen2.5-coder:7b` | ≈ 4.7 GB | Best local option for code review and tests on 16 GB laptops |
| `qwen2.5-coder:1.5b` | smaller | Low-RAM machines; reduced quality |
| `qwen3:4b` | ≈ 2.5 GB | Quick questions and simple explanations; may fail on complex structured JSON (review and tests) |

```bash
ollama pull qwen2.5-coder:7b
ollama pull qwen3:4b
```

**Which one to choose, by RAM:**

| RAM | Recommendation |
|---|---|
| 8 GB or less | Groq (cloud). Locally, only 1.5B models, with reduced quality |
| 16 GB | Groq for daily use and `qwen2.5-coder:7b` as the offline plan |
| 32 GB or more | Larger local models, depending on availability in Ollama |

Without a dedicated GPU, local models run on the CPU and can take from tens of seconds to minutes per answer. If the JSON comes back broken with a small model, use a larger one or go back to Groq.

### 2. Development agent (OpenCode)

OpenCode is used to evolve the code. It can use the models from its own free catalog, API keys from providers such as Groq or Nvidia NIM, or local Ollama models. Details in [Developing with OpenCode](#developing-with-opencode-and-other-ai-agents).

---

## Getting started

### Prerequisites

- **JDK 21+** and **Maven 3.9+**
- A free Groq key **or** Ollama installed with a downloaded model
- Docker (optional)

Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven git curl
```

### 1. Clone

```bash
git clone https://github.com/tpsousa/codementor-ai.git
cd codementor-ai
```

### 2. Configure the AI

**Option A: Groq (cloud, default)**

```bash
export AI_API_KEY=your-groq-key
```

Windows (PowerShell): `$env:AI_API_KEY="your-groq-key"`

**Option B: Ollama (local, no key)**

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama pull qwen2.5-coder:7b

export AI_BASE_URL=http://localhost:11434
export AI_COMPLETIONS_PATH=/v1/chat/completions
export AI_API_KEY=ollama
export AI_MODEL=qwen2.5-coder:7b
```

Here `AI_API_KEY=ollama` is just a placeholder text required by the project, not a real key.

For other providers, see the [AI models](#ai-models) table or the `.env.example` file.

### 3. Run

```bash
mvn spring-boot:run
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

> Tip: generate the Maven Wrapper once with `mvn wrapper:wrapper` and use `./mvnw` afterwards.

### 4. First request

```bash
curl -X POST http://localhost:8080/api/v1/reviews \
  -H "Content-Type: application/json" \
  -d '{"language":"java","code":"public int dividir(int a, int b) { return a / b; }"}'
```

More ready-made examples in [`docs/requests.http`](docs/requests.http) (works in IntelliJ and in VS Code with the REST Client extension).

---

## Configuration

| Variable | Default | Description |
|---|---|---|
| `AI_BASE_URL` | `https://api.groq.com/openai` | Provider base URL |
| `AI_API_KEY` | `coloque-sua-chave` | API key (`ollama` for Ollama) |
| `AI_MODEL` | `llama-3.3-70b-versatile` | Model |
| `AI_COMPLETIONS_PATH` | `/v1/chat/completions` | Chat endpoint path |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` (H2) or `prod` (PostgreSQL) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | — | Required in the `prod` profile |
| `RATE_LIMIT_RPM` | `10` | Requests per minute per IP |
| `PORT` | `8080` | HTTP port |

### Migrations (Flyway)

The schema is versioned in `src/main/resources/db/migration` (V1 creates the `analysis` table).

- **`dev`**: the in-memory H2 database receives the migrations and `ddl-auto` stays `update`.
- **`prod`**: `ddl-auto: validate`. Hibernate only validates; any schema change requires a new migration.
- Existing production databases (created with `update`) are **baselined** at V1 (`baseline-on-migrate: true`), without recreating the table.

To evolve the schema, add a file such as `V2__description.sql` and restart the application.

---

## API endpoints

Base: `/api/v1`

| Method | Route | Description |
|---|---|---|
| `POST` | `/reviews` | AI code review |
| `POST` | `/tests` | Generates JUnit 5 tests |
| `POST` | `/explanations` | Explains code |
| `POST` | `/explanations/stream` | Explains code with streaming (SSE) |
| `GET` | `/history?page=0&size=10&type=REVIEW` | Paginated history (`type`: `REVIEW`, `TESTS`, `EXPLANATION`) |
| `GET` | `/history/{id}` | Details of one analysis |

### `POST /reviews`

```json
{ "language": "java", "code": "public int dividir(int a, int b) { return a / b; }" }
```

Response `200`:

```json
{
  "id": 1,
  "score": 5,
  "summary": "The method does not handle division by zero.",
  "issues": [
    {
      "severity": "HIGH",
      "line": 1,
      "description": "Division by zero throws ArithmeticException.",
      "suggestion": "Validate the divisor before the operation."
    }
  ],
  "improvedCode": "public int dividir(int a, int b) { ... }",
  "createdAt": "2026-01-15T14:30:00Z"
}
```

### `POST /tests`

```json
{ "className": "Calculadora", "code": "public class Calculadora { public int somar(int a, int b) { return a + b; } }" }
```

Response: `id`, `testClassName`, `testCode`, `scenariosCovered`, `createdAt`.

### `POST /explanations`

```json
{ "language": "java", "code": "list.stream().filter(n -> n % 2 == 0).toList();", "level": "BEGINNER" }
```

`level` is optional (default `BEGINNER`). Response: `id`, `level`, `explanation` (Markdown), `createdAt`.

### `POST /explanations/stream` (SSE)

Same body as `/explanations`, but the response is `text/event-stream`. Keep the connection open (in a terminal, use `curl -N`):

```bash
curl -N -X POST http://localhost:8080/api/v1/explanations/stream \
  -H "Content-Type: application/json" \
  -d '{"language":"java","code":"list.stream().filter(n -> n % 2 == 0).toList();","level":"BEGINNER"}'
```

| Event | `data` | When |
|---|---|---|
| `token` | a piece of text | each time the AI generates a chunk |
| `done` | `{"id":1,"level":"BEGINNER","createdAt":"..."}` | end of the stream; the analysis has already been saved to the history |
| `error` | `{"detail":"..."}` | provider failure during the stream |

The status is `200` (the stream has already started); validation errors still return `400` before the stream begins. On an error, or if the client cancels, the incomplete text is **not** saved to the history.

> Only explanations use streaming. Review and tests return structured JSON, which is only valid once complete.

### Errors (RFC 7807)

| Status | When |
|---|---|
| `400` | Invalid JSON, empty field, code over 8000 characters, invalid enum |
| `404` | Analysis not found in the history |
| `429` | Per-minute request limit exceeded |
| `502` | AI provider unavailable, invalid key or response in the wrong format |

```json
{ "type": "about:blank", "title": "Bad Request", "status": 400, "detail": "'code': O código não pode ser vazio" }
```

---

## Project structure

```
codementor-ai/
├── .github/workflows/ci.yml
├── docs/requests.http
├── src/
│   ├── main/
│   │   ├── java/com/codementor/
│   │   │   ├── CodeMentorApplication.java
│   │   │   ├── ai/AiAssistant.java              # single entry point to the AI
│   │   │   ├── config/                          # OpenApiConfig, RateLimitFilter
│   │   │   ├── controller/                      # Review, TestGen, Explanation, History
│   │   │   ├── domain/                          # Analysis (entity), AnalysisType
│   │   │   ├── dto/                             # request/response/AI records
│   │   │   ├── exception/                       # GlobalExceptionHandler + exceptions
│   │   │   ├── repository/AnalysisRepository.java
│   │   │   └── service/                         # business rules + AnalysisRecorder
│   │   └── resources/
│   │       ├── application.yml / -dev.yml / -prod.yml
│   │       ├── db/migration/V1__cria_tabela_analysis.sql
│   │       └── prompts/ review.st, generate-tests.st, explain.st
│   └── test/java/com/codementor/                # unit and controller tests
├── .env.example  .gitignore  .dockerignore
├── AGENTS.md  CLAUDE.md
├── Dockerfile  docker-compose.yml
├── LICENSE  pom.xml  README.md  README.pt-BR.md
```

---

## How the AI is integrated

**A single dependency** (`pom.xml`): `spring-ai-starter-model-openai`. Because Groq, Gemini and Ollama speak the OpenAI protocol, the provider changes only in `application.yml` / environment variables:

```yaml
spring:
  ai:
    openai:
      base-url: ${AI_BASE_URL:https://api.groq.com/openai}
      api-key: ${AI_API_KEY:coloque-sua-chave}
      chat:
        completions-path: ${AI_COMPLETIONS_PATH:/v1/chat/completions}
        options:
          model: ${AI_MODEL:llama-3.3-70b-versatile}
          temperature: 0.2
```

**Structured output**: `AiAssistant` asks the model to answer in JSON and Spring AI converts it straight into a record:

```java
chatClient.prompt()
    .user(u -> u.text(promptTemplate).params(variables))
    .call()
    .entity(ReviewResult.class);   // JSON → Java record
```

**Prompts in files**: `src/main/resources/prompts/review.st`. Variables use `{name}`, so prompts **must not** contain literal curly braces.

---

## Tests

```bash
mvn test
```

| Test | What it covers |
|---|---|
| `ReviewServiceTest` | Service flow with a mocked AI and score clamping (0–10) |
| `TestGenServiceTest` | Test generation, prompt variables and missing scenarios |
| `ExplanationServiceTest` | Explanation by level, default level and streaming with persistence at the end |
| `ExplanationControllerTest` | SSE streaming (`token`, `done`, `error`) and `400` |
| `HistoryServiceTest` | Listing with/without filter, capped pagination and `404` |
| `ReviewControllerTest` | Status `200`, `400` (validation) and `502` (AI unavailable) |

The AI is **always mocked** in tests: they use no quota and are deterministic.

---

## Docker

```bash
cp .env.example .env            # fill in AI_API_KEY
docker compose up -d --build    # API + PostgreSQL
```

To use Ollama inside the compose setup:

```bash
# in .env: AI_BASE_URL=http://ollama:11434  AI_API_KEY=ollama  AI_MODEL=qwen2.5-coder:1.5b
docker compose --profile ollama up -d --build
docker compose exec ollama ollama pull qwen2.5-coder:1.5b
```

---

## Developing with OpenCode and other AI agents

The project is developed with **[OpenCode](https://opencode.ai)**, an open source (MIT) coding agent for the terminal. It reads the project, edits files, runs commands (such as `mvn test`) and supports many model providers, including local models.

**Why OpenCode:** it does not lock you into a single vendor. If a provider changes its free plan, you just switch models in the same tool. This is what happened with Gemini CLI, whose free access for individual accounts ended on June 18, 2026 and was replaced by the closed-source Antigravity CLI.

### Models used in OpenCode

| Source | Models (examples) | Notes |
|---|---|---|
| **OpenCode free catalog (Zen)** | DeepSeek V4 Flash Free, Nemotron 3 Ultra Free, MiMo-V2.5 Free, Big Pickle | Free for a limited time; the list changes. Use `/models` inside OpenCode to see what is available |
| **Nvidia NIM** (free key) | Models such as GLM, DeepSeek and Nemotron, per the current catalog | Requires creating a key at build.nvidia.com |
| **Groq** (free key) | `llama-3.3-70b-versatile` | Fast; subject to free-plan limits |
| **Ollama (local)** | `qwen2.5-coder:7b`, `qwen3:4b` | Offline and private; small models often fail on long agent tasks |

Important notes:

- The free Zen models are intended for use **inside OpenCode**. Do not use them as the project's `AI_BASE_URL`; for that, use Groq, Ollama or another provider from the [table above](#1-api-engine-configurable-providers).
- GLM 5.1 appears as **paid** on Zen; to use it for free, check whether it is available through Nvidia NIM.
- Free cloud models may use your prompts to improve the service. **Do not** use them with confidential code; use Ollama for that.

### Installation and usage (Ubuntu)

Check the current installation command at [opencode.ai](https://opencode.ai). Generally:

```bash
curl -fsSL https://opencode.ai/install | bash
cd codementor-ai
opencode
```

Inside OpenCode, use `/models` to pick a model. For agent tasks (editing several files, debugging, running tests), prefer a free cloud model or Groq; keep Ollama for offline work or sensitive code.

### Agent instruction files

- **`AGENTS.md`**: instructions for coding agents such as OpenCode and OpenAI Codex.
- **`CLAUDE.md`**: the same conventions, kept for Claude Code compatibility.

Main rules: AI calls only through `AiAssistant`, prompts in `prompts/*.st`, tests never call the real AI, and no API key goes into the code.

### Recommended workflow

```bash
git init && git add . && git commit -m "chore: initial version"   # a point to return to
opencode                                                           # ask for one task at a time
git diff && mvn -q test                                            # review and test
git add . && git commit -m "feat: describe the change"
```

If the agent breaks something: `git checkout .` goes back to the last commit.

### Task ideas to evolve the project

1. *"Add explanation caching with Caffeine to save quota."*
2. *"Write Testcontainers integration tests for the history."*
3. *"Add an endpoint that receives a GitHub Pull Request URL and reviews the diff."*
4. *"Add cursor-based pagination to the history."*

Always ask for one task at a time, review the diff and run the tests before moving on.

---

## Security

- **Never** commit API keys (`.env` is already in `.gitignore`).
- The submitted code is sent to the AI provider. For sensitive code, use **local Ollama**.
- Free cloud models (including OpenCode's and the Gemini free tier) may use your prompts to improve the service; check each one's terms.
- The 8000-character limit per request and the per-IP rate limit prevent abuse and quota exhaustion.
- The API has **no authentication** yet. Before exposing it publicly, add Spring Security/JWT (see the Roadmap).
- The rate limiter is in-memory (single instance). For multiple instances, use Redis.
- When using an AI agent, do not let it read the `.env` file.

---

## Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| `502 Bad Gateway` | Invalid key, provider down or wrong model | Check `AI_API_KEY`, `AI_MODEL` and the application logs |
| Provider `404` error | Wrong `AI_COMPLETIONS_PATH` | Gemini: `/chat/completions`; Groq/Ollama: `/v1/chat/completions` |
| `429` | Local rate limit or free-tier quota | Wait or raise `RATE_LIMIT_RPM` |
| Invalid JSON from the AI | Model too small (e.g. `qwen3:4b`) | Use a larger model (`llama-3.3-70b-versatile`, `qwen2.5-coder:7b`) |
| Slow answers on Ollama | Large model running on CPU | Use `qwen2.5-coder:1.5b` or a cloud provider |
| `Connection refused` on `localhost:11434` | Ollama is not running | Run `ollama serve` |
| Free model disappeared in OpenCode | Free models are temporary | Use `/models` and pick another, or connect Groq/Ollama |
| AI agent gets lost on long tasks | Small local model | Switch to a free cloud model |

---

## Roadmap

- [x] Code review with structured output
- [x] JUnit test generator
- [x] Code explainer
- [x] Paginated history
- [x] Rate limiting, Docker and CI
- [x] Response streaming (SSE)
- [x] Flyway migrations
- [ ] Automatic Pull Request review via GitHub Webhooks
- [ ] RAG over the repository (pgvector)
- [ ] JWT authentication and per-user quotas
- [ ] Web interface (React)
- [ ] Token and latency metrics (Micrometer)

---

## Contributing

1. Fork the project
2. `git checkout -b feature/my-feature`
3. Commit using [Conventional Commits](https://www.conventionalcommits.org/): `git commit -m "feat: add X"`
4. `mvn verify`
5. `git push origin feature/my-feature` and open a **Pull Request**

---

## License

Distributed under the **MIT** license. See [`LICENSE`](LICENSE).

---

## Author

Made by **Your Name**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?logo=linkedin&logoColor=white)](https://linkedin.com/in/your-username)
[![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white)](https://github.com/tpsousa)

If this project helped you, leave a star on the repository.
![License](https://img.shields.io/badge/license-MIT-blue)

---

## Sumário

1. [Sobre o projeto](#sobre-o-projeto)
2. [Por que estas escolhas](#por-que-estas-escolhas)
3. [Funcionalidades](#funcionalidades)
4. [Arquitetura](#arquitetura)
5. [Modelos de IA](#modelos-de-ia)
6. [Como executar](#como-executar)
7. [Configuração](#configuração)
8. [Endpoints da API](#endpoints-da-api)
9. [Estrutura de pastas](#estrutura-de-pastas)
10. [Como a IA é integrada](#como-a-ia-é-integrada)
11. [Testes](#testes)
12. [Docker](#docker)
13. [Desenvolvendo com OpenCode e outros agentes de IA](#desenvolvendo-com-opencode-e-outros-agentes-de-ia)
14. [Segurança](#segurança)
15. [Problemas comuns](#problemas-comuns)
16. [Roadmap](#roadmap)
17. [Como contribuir](#como-contribuir)
18. [Licença](#licença)

---

## Sobre o projeto

Projeto de portfólio que mostra como integrar **LLMs** em uma aplicação Java de forma limpa e profissional:

- Integração com IA via **Spring AI** (`ChatClient`), sem SDK proprietário.
- **Saídas estruturadas**: a IA responde em JSON, convertido automaticamente em `records` Java.
- **Prompts versionados** em arquivos `.st`, separados do código.
- **Provedor intercambiável** (Groq, Gemini, Ollama, OpenAI) por configuração.
- **Streaming (SSE)** nas explicações, com persistência ao final do stream.
- Persistência (JPA + Flyway), validação, erros no padrão RFC 7807, OpenAPI/Swagger, rate limit, testes, Docker e CI.

**Problema que resolve:** devs juniores e times pequenos nem sempre têm quem revise código ou escreva testes. O CodeMentor AI é um revisor disponível 24/7 que explica **o que está errado, por quê e como corrigir**.

---

## Por que estas escolhas

| Decisão | Motivo |
|---|---|
| **Groq como provedor padrão da API** | Free tier sem cartão, muito rápido, modelo `llama-3.3-70b-versatile` produz JSON confiável e a API é compatível com OpenAI |
| **Ollama como alternativa local** | Roda offline, sem limite e sem enviar seu código para a nuvem |
| **`qwen2.5-coder:7b` como modelo local recomendado** | Especializado em código e cabe em notebooks com 16 GB de RAM |
| **Gemini como opção extra** | Free tier com limites que variam; confira a documentação atual |
| **Spring AI em vez de SDK do provedor** | Abstração única; trocar de provedor = mudar variáveis de ambiente |
| **Um único starter (`spring-ai-starter-model-openai`)** | Groq, Gemini e Ollama expõem API compatível com OpenAI |
| **`AiAssistant` como porta única** | Isola a IA, centraliza erros e facilita mockar nos testes |
| **H2 no dev, PostgreSQL no prod** | Rodar sem instalar nada, mas pronto para produção |
| **Rate limit próprio (sem dependência extra)** | Protege a cota gratuita; simples de entender |
| **OpenCode como agente de desenvolvimento** | Open source (MIT), aceita vários provedores e modelos locais, sem depender de um único fornecedor |

---

## Funcionalidades

| Recurso | Descrição |
|---|---|
| **Code Review** | Retorna nota 0–10, problemas com severidade, sugestões e código melhorado |
| **Gerador de testes** | Gera testes JUnit 5 + Mockito para uma classe Java |
| **Explicador** | Explica código para nível `BEGINNER`, `INTERMEDIATE` ou `ADVANCED` |
| **Streaming (SSE)** | Explicações chegam em pedaços via `text/event-stream` |
| **Histórico** | Todas as análises ficam salvas, com paginação e filtro por tipo |
| **Multi-provedor** | Groq, Gemini, Ollama ou OpenAI via variáveis de ambiente |
| **Swagger UI** | Documentação interativa em `/swagger-ui.html` |
| **Rate limit** | Limite de requisições por IP/minuto para proteger sua cota |

---

## Arquitetura

```
Cliente ──▶ Controller ──▶ Service ──▶ AiAssistant ──▶ Spring AI ChatClient ──▶ Groq / Gemini / Ollama
 (REST)     (validação,    (regras)        (porta           │
             DTOs)             │            única da IA)    └─ prompts/*.st
                               ▼
                        AnalysisRecorder ──▶ Repository (JPA) ──▶ H2 (dev) / PostgreSQL (prod)
```

Fluxo de um code review:

1. `POST /api/v1/reviews` chega ao `ReviewController`, que valida o JSON (`@Valid`).
2. `ReviewService` monta as variáveis e chama `AiAssistant.ask(...)` com o prompt `review.st`.
3. O Spring AI envia o prompt ao provedor e converte a resposta JSON em `ReviewResult`.
4. `AnalysisRecorder` salva no histórico e o service devolve `ReviewResponse` (nota limitada a 0–10).
5. Qualquer falha da IA vira `502 Bad Gateway` via `GlobalExceptionHandler`.

---

## Modelos de IA

O projeto usa IA em **dois papéis diferentes**, que não devem ser confundidos:

1. **Motor da API:** o modelo que o CodeMentor chama em tempo de execução para revisar, testar e explicar código (configurado por variáveis de ambiente).
2. **Agente de desenvolvimento:** a ferramenta de terminal usada para escrever e evoluir o código do projeto (OpenCode, descrito [mais abaixo](#desenvolvendo-com-opencode-e-outros-agentes-de-ia)).

### 1. Motor da API (provedores configuráveis)

> **Atenção:** limites e nomes de modelos dos *free tiers* mudam com frequência. Confira a documentação oficial de cada provedor.

| Provedor | Custo | Chave | `AI_BASE_URL` | `AI_COMPLETIONS_PATH` | Exemplo de `AI_MODEL` |
|---|---|---|---|---|---|
| **Groq** (padrão) | Free tier | [console.groq.com](https://console.groq.com) | `https://api.groq.com/openai` | `/v1/chat/completions` | `llama-3.3-70b-versatile` |
| **Ollama** (local) | Grátis, ilimitado | não precisa (use `ollama`) | `http://localhost:11434` | `/v1/chat/completions` | `qwen2.5-coder:7b` |
| **Google Gemini** | Free tier (confira os limites atuais) | [aistudio.google.com](https://aistudio.google.com) | `https://generativelanguage.googleapis.com/v1beta/openai` | `/chat/completions` | consulte a lista atual de modelos |
| **OpenAI** | Pago | [platform.openai.com](https://platform.openai.com) | `https://api.openai.com` | `/v1/chat/completions` | `gpt-4o-mini` |

**Modelos locais testados neste projeto (Ollama):**

| Modelo | Tamanho aproximado | Uso recomendado |
|---|---|---|
| `qwen2.5-coder:7b` | ≈ 4,7 GB | Melhor opção local para code review e testes em notebooks de 16 GB |
| `qwen2.5-coder:1.5b` | menor | Máquinas com pouca RAM; qualidade reduzida |
| `qwen3:4b` | ≈ 2,5 GB | Perguntas rápidas e explicações simples; pode falhar em JSON estruturado complexo (review e testes) |

```bash
ollama pull qwen2.5-coder:7b
ollama pull qwen3:4b
```

**Qual escolher conforme a RAM do computador:**

| RAM | Recomendação |
|---|---|
| 8 GB ou menos | Groq (nuvem). Localmente, só modelos de 1,5B, com qualidade reduzida |
| 16 GB | Groq no dia a dia e `qwen2.5-coder:7b` como plano offline |
| 32 GB ou mais | Modelos locais maiores, conforme a disponibilidade no Ollama |

Sem GPU dedicada, os modelos locais rodam na CPU e podem levar de dezenas de segundos a minutos por resposta. Se o JSON vier quebrado com um modelo pequeno, use um modelo maior ou volte para o Groq.

### 2. Agente de desenvolvimento (OpenCode)

O OpenCode é usado para evoluir o código. Ele pode usar os modelos gratuitos do próprio catálogo, a chave de provedores como Groq ou Nvidia NIM, ou os modelos locais do Ollama. Detalhes na seção [Desenvolvendo com OpenCode](#desenvolvendo-com-opencode-e-outros-agentes-de-ia).

---

## Como executar

### Pré-requisitos

- **JDK 21+** e **Maven 3.9+**
- Uma chave gratuita do Groq **ou** Ollama instalado com um modelo baixado
- Docker (opcional)

Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven git curl
```

### 1. Clonar

```bash
git clone https://github.com/tpsousa/codementor-ai.git
cd codementor-ai
```

### 2. Configurar a IA

**Opção A: Groq (nuvem, padrão)**

```bash
export AI_API_KEY=sua-chave-groq
```

Windows (PowerShell): `$env:AI_API_KEY="sua-chave-groq"`

**Opção B: Ollama (local, sem chave)**

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama pull qwen2.5-coder:7b

export AI_BASE_URL=http://localhost:11434
export AI_COMPLETIONS_PATH=/v1/chat/completions
export AI_API_KEY=ollama
export AI_MODEL=qwen2.5-coder:7b
```

Aqui `AI_API_KEY=ollama` é só um texto exigido pelo projeto, não é uma chave real.

Para outros provedores, veja a tabela de [modelos de IA](#modelos-de-ia) ou o arquivo `.env.example`.

### 3. Rodar

```bash
mvn spring-boot:run
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

> Dica: gere o Maven Wrapper uma vez com `mvn wrapper:wrapper` e use `./mvnw` depois.

### 4. Primeira requisição

```bash
curl -X POST http://localhost:8080/api/v1/reviews \
  -H "Content-Type: application/json" \
  -d '{"language":"java","code":"public int dividir(int a, int b) { return a / b; }"}'
```

Mais exemplos prontos em [`docs/requests.http`](docs/requests.http) (funciona no IntelliJ e no VS Code com REST Client).

---

## Configuração

| Variável | Padrão | Descrição |
|---|---|---|
| `AI_BASE_URL` | `https://api.groq.com/openai` | URL base do provedor |
| `AI_API_KEY` | `coloque-sua-chave` | Chave da API (`ollama` para Ollama) |
| `AI_MODEL` | `llama-3.3-70b-versatile` | Modelo |
| `AI_COMPLETIONS_PATH` | `/v1/chat/completions` | Caminho do endpoint de chat |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` (H2) ou `prod` (PostgreSQL) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | — | Obrigatórias no perfil `prod` |
| `RATE_LIMIT_RPM` | `10` | Requisições por minuto por IP |
| `PORT` | `8080` | Porta HTTP |

### Migrations (Flyway)

O schema é versionado em `src/main/resources/db/migration` (a V1 cria a tabela `analysis`).

- **`dev`**: o H2 em memória recebe as migrations e o `ddl-auto` continua `update`.
- **`prod`**: `ddl-auto: validate`. O Hibernate apenas valida; qualquer mudança de schema exige uma nova migration.
- Bancos de produção já existentes (criados com `update`) são **baselinados** na V1 (`baseline-on-migrate: true`), sem recriar a tabela.

Para evoluir o schema, adicione um arquivo como `V2__descricao.sql` e reinicie a aplicação.

---

## Endpoints da API

Base: `/api/v1`

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/reviews` | Code review com IA |
| `POST` | `/tests` | Gera testes JUnit 5 |
| `POST` | `/explanations` | Explica código |
| `POST` | `/explanations/stream` | Explica código em streaming (SSE) |
| `GET` | `/history?page=0&size=10&type=REVIEW` | Histórico paginado (`type`: `REVIEW`, `TESTS`, `EXPLANATION`) |
| `GET` | `/history/{id}` | Detalhe de uma análise |

### `POST /reviews`

```json
{ "language": "java", "code": "public int dividir(int a, int b) { return a / b; }" }
```

Resposta `200`:

```json
{
  "id": 1,
  "score": 5,
  "summary": "O método não trata divisão por zero.",
  "issues": [
    {
      "severity": "HIGH",
      "line": 1,
      "description": "Divisão por zero lança ArithmeticException.",
      "suggestion": "Valide o divisor antes da operação."
    }
  ],
  "improvedCode": "public int dividir(int a, int b) { ... }",
  "createdAt": "2026-01-15T14:30:00Z"
}
```

### `POST /tests`

```json
{ "className": "Calculadora", "code": "public class Calculadora { public int somar(int a, int b) { return a + b; } }" }
```

Resposta: `id`, `testClassName`, `testCode`, `scenariosCovered`, `createdAt`.

### `POST /explanations`

```json
{ "language": "java", "code": "list.stream().filter(n -> n % 2 == 0).toList();", "level": "BEGINNER" }
```

`level` é opcional (padrão `BEGINNER`). Resposta: `id`, `level`, `explanation` (Markdown), `createdAt`.

### `POST /explanations/stream` (SSE)

Mesmo corpo do `/explanations`, mas a resposta é `text/event-stream`. Mantenha a conexão aberta (no terminal, use `curl -N`):

```bash
curl -N -X POST http://localhost:8080/api/v1/explanations/stream \
  -H "Content-Type: application/json" \
  -d '{"language":"java","code":"list.stream().filter(n -> n % 2 == 0).toList();","level":"BEGINNER"}'
```

| Evento | `data` | Quando |
|---|---|---|
| `token` | pedaço do texto | a cada trecho gerado pela IA |
| `done` | `{"id":1,"level":"BEGINNER","createdAt":"..."}` | fim do stream; a análise já foi salva no histórico |
| `error` | `{"detail":"..."}` | falha do provedor durante o stream |

O status é `200` (o stream já começou); erros de validação continuam retornando `400` antes do stream. Em caso de erro ou cancelamento pelo cliente, o texto incompleto **não** é salvo no histórico.

> Apenas as explicações usam streaming. Review e testes devolvem JSON estruturado, que só é válido quando completo.

### Erros (RFC 7807)

| Status | Quando |
|---|---|
| `400` | JSON inválido, campo vazio, código acima de 8000 caracteres, enum inválido |
| `404` | Análise não encontrada no histórico |
| `429` | Limite de requisições por minuto excedido |
| `502` | Provedor de IA indisponível, chave inválida ou resposta fora do formato |

```json
{ "type": "about:blank", "title": "Bad Request", "status": 400, "detail": "'code': O código não pode ser vazio" }
```

---

## Estrutura de pastas

```
codementor-ai/
├── .github/workflows/ci.yml
├── docs/requests.http
├── src/
│   ├── main/
│   │   ├── java/com/codementor/
│   │   │   ├── CodeMentorApplication.java
│   │   │   ├── ai/AiAssistant.java              # porta única para a IA
│   │   │   ├── config/                          # OpenApiConfig, RateLimitFilter
│   │   │   ├── controller/                      # Review, TestGen, Explanation, History
│   │   │   ├── domain/                          # Analysis (entidade), AnalysisType
│   │   │   ├── dto/                             # records de request/response/IA
│   │   │   ├── exception/                       # GlobalExceptionHandler + exceções
│   │   │   ├── repository/AnalysisRepository.java
│   │   │   └── service/                         # regras de negócio + AnalysisRecorder
│   │   └── resources/
│   │       ├── application.yml / -dev.yml / -prod.yml
│   │       ├── db/migration/V1__cria_tabela_analysis.sql
│   │       └── prompts/ review.st, generate-tests.st, explain.st
│   └── test/java/com/codementor/                # testes unitários e de controller
├── .env.example  .gitignore  .dockerignore
├── AGENTS.md  CLAUDE.md
├── Dockerfile  docker-compose.yml
├── LICENSE  pom.xml  README.md  README.pt-BR.md
```

---

## Como a IA é integrada

**Dependência única** (`pom.xml`): `spring-ai-starter-model-openai`. Como Groq, Gemini e Ollama falam o protocolo da OpenAI, o provedor muda só em `application.yml` / variáveis de ambiente:

```yaml
spring:
  ai:
    openai:
      base-url: ${AI_BASE_URL:https://api.groq.com/openai}
      api-key: ${AI_API_KEY:coloque-sua-chave}
      chat:
        completions-path: ${AI_COMPLETIONS_PATH:/v1/chat/completions}
        options:
          model: ${AI_MODEL:llama-3.3-70b-versatile}
          temperature: 0.2
```

**Saída estruturada**: o `AiAssistant` pede ao modelo que responda em JSON e o Spring AI converte direto para um record:

```java
chatClient.prompt()
    .user(u -> u.text(promptTemplate).params(variables))
    .call()
    .entity(ReviewResult.class);   // JSON → record Java
```

**Prompts em arquivo**: `src/main/resources/prompts/review.st`. As variáveis usam `{nome}`; por isso os prompts **não** podem ter chaves literais.

---

## Testes

```bash
mvn test
```

| Teste | O que cobre |
|---|---|
| `ReviewServiceTest` | Fluxo do service com IA mockada e limite da nota (0–10) |
| `TestGenServiceTest` | Geração de testes, variáveis do prompt e cenários ausentes |
| `ExplanationServiceTest` | Explicação por nível, nível padrão e streaming com persistência ao final |
| `ExplanationControllerTest` | Streaming SSE (`token`, `done`, `error`) e `400` |
| `HistoryServiceTest` | Listagem com/sem filtro, paginação limitada e `404` |
| `ReviewControllerTest` | Status `200`, `400` (validação) e `502` (IA indisponível) |

A IA é **sempre mockada** nos testes: não gastam cota e são determinísticos.

---

## Docker

```bash
cp .env.example .env            # preencha AI_API_KEY
docker compose up -d --build    # API + PostgreSQL
```

Para usar Ollama dentro do compose:

```bash
# no .env: AI_BASE_URL=http://ollama:11434  AI_API_KEY=ollama  AI_MODEL=qwen2.5-coder:1.5b
docker compose --profile ollama up -d --build
docker compose exec ollama ollama pull qwen2.5-coder:1.5b
```

---

## Desenvolvendo com OpenCode e outros agentes de IA

O projeto é desenvolvido com o **[OpenCode](https://opencode.ai)**, um agente de programação open source (MIT) para o terminal. Ele lê o projeto, edita arquivos, roda comandos (como `mvn test`) e aceita vários provedores de modelos, inclusive modelos locais.

**Por que OpenCode:** não prende você a um único fornecedor. Se um provedor mudar o plano gratuito, basta trocar de modelo no mesmo programa. Isso aconteceu com o Gemini CLI, cujo acesso gratuito para contas individuais foi encerrado em 18 de junho de 2026, com substituição pelo Antigravity CLI, de código fechado.

### Modelos usados no OpenCode

| Origem | Modelos (exemplos) | Observações |
|---|---|---|
| **Catálogo gratuito do OpenCode (Zen)** | DeepSeek V4 Flash Free, Nemotron 3 Ultra Free, MiMo-V2.5 Free, Big Pickle | Gratuitos por tempo limitado; a lista muda. Use `/models` dentro do OpenCode para ver o que está liberado |
| **Nvidia NIM** (chave gratuita) | Modelos como GLM, DeepSeek e Nemotron, conforme o catálogo atual | Requer criar uma chave em build.nvidia.com |
| **Groq** (chave gratuita) | `llama-3.3-70b-versatile` | Rápido; sujeito aos limites do plano gratuito |
| **Ollama (local)** | `qwen2.5-coder:7b`, `qwen3:4b` | Offline e privado; modelos pequenos costumam falhar em tarefas longas de agente |

Notas importantes:

- Os modelos gratuitos do Zen são pensados para uso **dentro do OpenCode**. Não os use como `AI_BASE_URL` da API do projeto; para isso, use Groq, Ollama ou outro provedor da [tabela acima](#1-motor-da-api-provedores-configuráveis).
- O GLM 5.1 aparece como **pago** no Zen; para usá-lo de graça, verifique se está disponível via Nvidia NIM.
- Modelos gratuitos na nuvem podem usar seus prompts para melhorar o serviço. **Não** use com código confidencial; para isso, use o Ollama.

### Instalação e uso (Ubuntu)

Confira o comando de instalação atual em [opencode.ai](https://opencode.ai). Em geral:

```bash
curl -fsSL https://opencode.ai/install | bash
cd codementor-ai
opencode
```

Dentro do OpenCode, use `/models` para escolher o modelo. Para tarefas de agente (editar vários arquivos, depurar, rodar testes), prefira um modelo gratuito da nuvem ou o Groq; deixe o Ollama para trabalho offline ou código sensível.

### Arquivos de instruções do agente

- **`AGENTS.md`**: instruções para agentes de programação, como o OpenCode e o OpenAI Codex.
- **`CLAUDE.md`**: mesmas convenções, mantidas para compatibilidade com o Claude Code.

As regras principais: chamadas à IA só via `AiAssistant`, prompts em `prompts/*.st`, testes nunca chamam a IA real e nenhuma chave de API vai para o código.

### Fluxo de trabalho recomendado

```bash
git init && git add . && git commit -m "chore: versão inicial"   # ponto de retorno
opencode                                                          # peça uma tarefa por vez
git diff && mvn -q test                                           # revise e teste
git add . && git commit -m "feat: descrição da mudança"
```

Se o agente estragar algo: `git checkout .` volta ao último commit.

### Ideias de tarefas para evoluir o projeto

1. *"Adicione cache de explicações com Caffeine para economizar cota."*
2. *"Escreva testes de integração com Testcontainers para o histórico."*
3. *"Adicione um endpoint que recebe a URL de um Pull Request do GitHub e revisa o diff."*
4. *"Adicione paginação por cursor no histórico."*

Peça sempre uma tarefa por vez, revise o diff e rode os testes antes de seguir.

---

## Segurança

- **Nunca** versione chaves de API (`.env` já está no `.gitignore`).
- O código enviado é transmitido ao provedor de IA. Para código sensível, use **Ollama local**.
- Modelos gratuitos na nuvem (inclusive os do OpenCode e o free tier do Gemini) podem usar seus prompts para melhorar o serviço; confira os termos de cada um.
- Limite de 8000 caracteres por requisição e rate limit por IP evitam abuso e estouro de cota.
- A API **não tem autenticação** ainda. Antes de expor publicamente, adicione Spring Security/JWT (veja o Roadmap).
- O rate limit é em memória (uma instância). Para várias instâncias, use Redis.
- Ao usar um agente de IA, não deixe que ele leia o arquivo `.env`.

---

## Problemas comuns

| Problema | Causa provável | Solução |
|---|---|---|
| `502 Bad Gateway` | Chave inválida, provedor fora do ar ou modelo errado | Confira `AI_API_KEY`, `AI_MODEL` e os logs da aplicação |
| Erro `404` do provedor | `AI_COMPLETIONS_PATH` errado | Gemini: `/chat/completions`; Groq/Ollama: `/v1/chat/completions` |
| `429` | Rate limit local ou cota do free tier | Aguarde ou aumente `RATE_LIMIT_RPM` |
| JSON inválido da IA | Modelo pequeno demais (ex.: `qwen3:4b`) | Use um modelo maior (`llama-3.3-70b-versatile`, `qwen2.5-coder:7b`) |
| Respostas lentas no Ollama | Modelo grande rodando em CPU | Use `qwen2.5-coder:1.5b` ou um provedor em nuvem |
| `Connection refused` em `localhost:11434` | Ollama não está rodando | Execute `ollama serve` |
| Modelo gratuito sumiu no OpenCode | Modelos gratuitos são temporários | Use `/models` e escolha outro, ou ligue o Groq/Ollama |
| Agente de IA se perde em tarefas longas | Modelo local pequeno | Troque para um modelo gratuito da nuvem |

---

## Roadmap

- [x] Code review com saída estruturada
- [x] Gerador de testes JUnit
- [x] Explicador de código
- [x] Histórico paginado
- [x] Rate limit, Docker e CI
- [x] Streaming de resposta (SSE)
- [x] Migrations com Flyway
- [ ] Revisão automática de Pull Requests via GitHub Webhooks
- [ ] RAG sobre o repositório (pgvector)
- [ ] Autenticação JWT e cotas por usuário
- [ ] Interface web (React)
- [ ] Métricas de tokens e latência (Micrometer)

---

## Como contribuir

1. Faça um *fork*
2. `git checkout -b feature/minha-feature`
3. Commits no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/): `git commit -m "feat: adiciona X"`
4. `mvn verify`
5. `git push origin feature/minha-feature` e abra um **Pull Request**

---

## Licença

Distribuído sob a licença **MIT**. Veja [`LICENSE`](LICENSE).

---

## Autor

Feito por **Seu Nome**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?logo=linkedin&logoColor=white)](https://linkedin.com/in/seu-usuario)
[![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white)](https://github.com/tpsousa)

Se este projeto te ajudou, deixe uma estrela no repositório.
