# CodeMentor AI

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
