# SCORM Package Generator — Backend

REST API backend for a course-authoring system that generates **SCORM 2004** packages, with AI features (Google Gemini) for generating course outlines, page content, and quizzes from source documents.

This is the backend of a graduation thesis project (HCMUT — semester 252). The frontend is a separate application that talks to this API under the `/api` context path.

## Key features

- **Course authoring**: manage course → section → page (content page / quiz page) → content block, plus theme config, thumbnails, and a media library.
- **SCORM 2004 export**: package a course into a SCORM-compliant `.zip` (imsmanifest, HTML/CSS/JS templates), stored on Cloudflare R2 and downloadable via the API.
- **AI (Google Gemini via Spring AI)**:
  - Generate a course outline from an uploaded document (PDF/DOCX/… parsed with Apache Tika) or from a text description.
  - Generate page content and quizzes, per page or for the whole course.
  - **RAG** with pgvector: source documents are chunked, embedded with `gemini-embedding-001` (768 dimensions), and the top-k most relevant chunks are retrieved for each generation. Set `AI_RAG_ENABLED=false` to fall back to full-document stuffing.
  - Runtime quality gate for quizzes (format, Bloom level, and citation checks) with automatic retry.
- **Auth & organizations**: register/login (JWT), Google login, organization management with members, resources, and notifications.

## Tech stack

| Component | Technology |
|---|---|
| Language / Framework | Java 17, Spring Boot 3.4 |
| AI | Spring AI 1.1 (Google GenAI — Gemini 2.5 Pro), Apache Tika |
| Database | PostgreSQL + pgvector, JPA/Hibernate, Flyway migrations |
| Storage | Cloudflare R2 (S3-compatible, AWS SDK v2) |
| Security | Spring Security + JWT (jjwt) |
| SCORM packaging | dom4j, commons-compress |
| Build / Deploy | Maven, Docker (multi-stage) |

## Prerequisites

- JDK 17+, Maven 3.9+
- PostgreSQL with the **pgvector** extension (required for RAG)
- A Google AI Studio API key
- A Cloudflare R2 account (or any S3-compatible storage)

## Setup & run

### 1. Configure the environment

Copy the template and fill in the values:

```bash
cp .env.example .env
```

Main variables (see [.env.example](.env.example) and [application.properties](src/main/resources/application.properties) for the full list):

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | PostgreSQL connection |
| `GOOGLE_GENAI_API_KEY`, `GOOGLE_GENAI_PROJECT_ID` | Google AI Studio |
| `APP_JWT_SECRET`, `APP_JWT_EXPIRATION_MS` | JWT |
| `APP_R2_ENDPOINT`, `APP_R2_BUCKET`, `APP_R2_ACCESS_KEY_ID`, `APP_R2_SECRET_ACCESS_KEY`, `APP_R2_PUBLIC_BASE_URL` | Cloudflare R2 |
| `AI_RAG_ENABLED`, `AI_RAG_TOP_K`, `AI_RAG_CHUNK_SIZE` | RAG tuning (optional) |
| `PORT` | Server port (default 8080) |

### 2. Run locally

```bash
mvn spring-boot:run
```

Flyway runs the migrations automatically on startup (scripts in [src/main/resources/db/migration/](src/main/resources/db/migration/)). The server listens at `http://localhost:8080/api`. Quick check: `GET /api/health`.

### 3. Run with Docker

```bash
docker build -t scorm-be .
docker run --env-file .env -p 8080:8080 scorm-be
```

### 4. Tests

```bash
mvn test
```

Load tests (k6) live in [load-tests/](load-tests/) — see [load-tests/README.md](load-tests/README.md).

## API overview

All endpoints are prefixed with `/api`. Except for `/auth/**` and `/health`, every endpoint requires an `Authorization: Bearer <token>` header.

| Group | Representative endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/google-login` |
| Course authoring | CRUD on `/courses`, `/sections`, `/pages`, `/content-pages`, `/content-blocks`, `/quiz-pages`, `/questions`, `/theme-configs` |
| AI | `POST /ai/generate-outline-from-file`, `/ai/generate-outline`, `/ai/save-outline`, `/ai/generate-page-content`, `/ai/generate-quiz`, `/ai/generate-course-quiz` |
| SCORM | `POST /courses/{courseId}/scorm-packages`, `GET /scorm-packages/{id}/download` |
| Other | `/organizations`, `/library`, `/media-assets`, `/notifications`, `/users` |

Full Postman collection: [postman_collection.json](postman_collection.json). Per-endpoint checklist: [api-checklist.md](api-checklist.md).

## Project structure

```
src/main/java/com/scorm/generator/
├── controller/       # REST controllers
├── service/          # Business logic
│   ├── ai/           #   Gemini-based content generation, quality gate
│   └── rag/          #   Chunking, embedding, vector search (pgvector)
├── ScormPackaging/   # SCORM 2004 packaging (manifest, zip)
├── entity/ dto/ repository/
├── security/         # JWT filter, Spring Security config
├── config/ exception/
src/main/resources/
├── db/migration/     # Flyway scripts
├── prompts/          # Prompt templates (outline, page-content, quiz)
├── scorm-template/   # HTML/CSS/JS templates for SCORM packages
docs/                 # Prompt engineering docs, AI evaluation, thesis report
document/             # Business/domain notes per entity and flow
load-tests/           # k6 stress/spike tests
```

## Further reading

- [docs/AI_PROMPT_ENGINEERING.md](docs/AI_PROMPT_ENGINEERING.md) — prompt design for the AI features
- [docs/giai-thich-tich-hop-LLM.md](docs/giai-thich-tich-hop-LLM.md) — LLM integration explained
- [docs/MOODLE_SCORM_VALIDATION_CHECKLIST.md](docs/MOODLE_SCORM_VALIDATION_CHECKLIST.md) — checklist for validating packages in Moodle
- [cach-tao-goi-scorm.md](cach-tao-goi-scorm.md) — how SCORM packages are built
- [document/](document/) — detailed notes on each business module
