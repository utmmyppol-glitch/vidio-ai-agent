# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vidio AI Agent — AI-powered ad content generation platform. Users input product info, and an async pipeline generates trend analysis → ad copy → video script → video (FFmpeg) → thumbnail (DALL-E). Built with Spring Boot 4 backend + React (Vite) frontend.

## Build & Run Commands

### Backend (Spring Boot, Java 17, Gradle)
```bash
./gradlew bootRun                    # Run with default profile (H2 in-memory DB)
./gradlew bootRun --args='--spring.profiles.active=local'  # Local profile
./gradlew build                      # Build (includes tests)
./gradlew bootJar -x test           # Build JAR without tests
./gradlew test                       # Run all tests
./gradlew test --tests "com.vidioaiagent.service.TrendAnalysisServiceTest"  # Single test class
./gradlew test --tests "*.AgentOrchestratorTest.testMethodName"             # Single test method
```

### Frontend (React 19, Vite 8, TypeScript)
```bash
cd frontend
npm install
npm run dev      # Dev server (proxied to backend :8080)
npm run build    # Production build (output: frontend/dist/)
npm run lint     # ESLint
```

### Docker (full stack: app + MySQL + Redis)
```bash
docker-compose up --build   # Requires ANTHROPIC_API_KEY and OPENAI_API_KEY env vars
```

## Architecture

### AI Pipeline (AgentOrchestrator)
The core flow is an async 5-step pipeline orchestrated by `AgentOrchestrator`:
1. **Trend Analysis** — `TrendAnalysisService` uses Spring AI ChatClient (Anthropic Claude) to analyze trends
2. **Ad Copy Generation** — `AdCopyService` generates title, description, hashtags, script, and timed subtitles
3. **Script Extraction** — Subtitles and script saved from ad copy step
4. **Video Generation** — `VideoGenerateService` uses FFmpeg to create subtitle overlay video (SRT-based)
5. **Thumbnail Generation** — `ThumbnailService` uses OpenAI DALL-E 3 for image generation

Pipeline progress is pushed to the frontend via **WebSocket (STOMP)** on `/topic/progress/{projectId}`. Frontend also uses HTTP polling as fallback.

### Key Backend Patterns
- **Spring AI integration**: `AiConfig` creates a `ChatClient` bean from `AnthropicChatModel` with a Korean-language system prompt
- **Prompt templates**: `PromptTemplates` utility class holds all AI prompt strings
- **Async execution**: Pipeline runs on `@Async` thread pool (core=5, max=10)
- **Profiles**: default (H2), `local`, `prod` (MySQL + Redis), `test` (H2 + disabled AI)
- **All endpoints are unauthenticated** (SecurityConfig permits all)

### API Endpoints
- `POST /api/projects` — Create project and start pipeline
- `GET /api/projects/{id}` — Poll project status
- `GET /api/projects` — List all projects
- `POST /api/projects/{id}/retry?step=` — Retry failed step
- `GET /api/projects/options` — Platform/style enum options
- `GET /api/files/video/{id}`, `GET /api/files/thumbnail/{id}` — File download
- Swagger UI: `/swagger-ui.html`

### Frontend Structure
- **State**: Zustand store (`useAppStore`) for current project and project list
- **Routing**: `/` (CreatePage), `/project/:id` (ProjectPage), `/history` (HistoryPage)
- **Real-time**: `useWebSocket` hook (STOMP) + `useProjectPolling` hook (HTTP fallback)
- **API client**: Axios with `/api` base path, 120s timeout
- **Styling**: Tailwind CSS v4 via `@tailwindcss/vite`

### External Dependencies
- **FFmpeg**: Required at runtime for video generation (installed in Docker image, must be on PATH for local dev)
- **Anthropic API**: Claude model for trend analysis and ad copy (required)
- **OpenAI API**: DALL-E 3 for thumbnail generation (optional, gracefully degrades)

## Profiles & Environment
| Profile | DB | Notes |
|---------|-----|-------|
| default | H2 in-memory | Dev, auto `create-drop` |
| local | H2 | Explicit local dev |
| prod | MySQL 8 | Docker compose, `ddl-auto=update` |
| test | H2 | AI key stubbed, for unit tests |

Required env vars for prod: `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `DB_HOST`, `DB_USER`, `DB_PASSWORD`.
