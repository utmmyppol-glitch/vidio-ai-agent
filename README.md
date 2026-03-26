# 🎬 Vidio AI Agent

> 상품 정보만 입력하면 **트렌드 분석 → 광고 카피 → 릴스/쇼츠 영상**까지 자동 생성하는 AI Agent

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=flat&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat&logo=react&logoColor=black)
![Claude AI](https://img.shields.io/badge/Claude-Sonnet%204-CC785C?style=flat)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql&logoColor=white)

---

## 📱 서비스 소개

사용자가 **상품명, 타겟 고객, 플랫폼, 스타일**만 입력하면 AI가 알아서:

1. 현재 트렌드를 분석하고
2. 숏폼에 최적화된 5씬 스크립트를 생성하고
3. Pexels 스톡 영상 + 자막을 합성해서
4. **바로 업로드 가능한 릴스/쇼츠 영상**을 만들어줍니다.

---

## 🔥 핵심 기능

### AI 파이프라인 (자동 실행)

```
[사용자 입력] → [트렌드 분석] → [씬 스크립트] → [영상 합성] → [완성]
```

| 단계 | 기능 | 기술 |
|------|------|------|
| 트렌드 분석 | 카테고리별 바이럴 포인트 추출 | Claude AI |
| 씬 스크립트 | HOOK→문제→변화→결과→CTA 5씬 공식 | Claude AI |
| 영상 합성 | Pexels 스톡 영상 + ASS 자막 오버레이 | Pexels API + FFmpeg |
| 썸네일 | Pexels 이미지 + 텍스트 오버레이 | Java2D |

### 숏폼 스크립트 공식

```
HOOK → 문제/궁금증 → 변화/실험 → 결과 → CTA
```

- 각 씬 15~25자 짧고 강하게
- 플랫폼별 톤 자동 적용 (릴스=감성, 쇼츠=직설, 틱톡=자극)
- 스타일별 훅 템플릿 (감성/자극/정보/유머/럭셔리/미니멀)
- 씬마다 Pexels 검색용 영어 키워드 자동 생성

### 지원 플랫폼

- 📱 유튜브 쇼츠 (9:16, 60초)
- 📸 인스타그램 릴스 (9:16, 90초)
- 🎵 틱톡 (9:16, 60초)
- 🎬 유튜브 일반 (16:9, 10분)

---

## 🛠️ 기술 스택

### Backend
- **Java 17** + **Spring Boot 4.0**
- **Spring AI** (Anthropic Claude 연동)
- **Spring Data JPA** + **MySQL**
- **WebSocket** (STOMP) — 실시간 진행상황
- **FFmpeg** — 영상 합성
- **Pexels API** — 스톡 영상/이미지

### Frontend
- **React 19** + **TypeScript**
- **Vite** — 빌드
- **Tailwind CSS v4**
- **Framer Motion** — 애니메이션
- **Zustand** — 상태관리
- **Axios** — API 통신

---

## 🚀 시작하기

### 1. 사전 요구사항

```bash
# Java 17
java -version

# MySQL
mysql --version

# FFmpeg (영상 생성용)
brew install ffmpeg

# Node.js (프론트엔드)
node -v
```

### 2. API 키 발급

| 서비스 | 용도 | 발급 |
|--------|------|------|
| Anthropic | AI 카피/분석 | https://console.anthropic.com |
| Pexels | 스톡 영상/이미지 | https://www.pexels.com/api |

### 3. MySQL 데이터베이스 생성

```bash
mysql -u root -e "CREATE DATABASE IF NOT EXISTS vidio_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 4. 환경변수 설정

```bash
export ANTHROPIC_API_KEY=your-key-here
```

`application.properties`에서 Pexels API 키 설정:
```properties
app.pexels.api-key=your-pexels-key
```

### 5. 백엔드 실행

```bash
cd vidio-ai-agent
./gradlew bootRun
```

### 6. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

### 7. 접속

- 프론트엔드: http://localhost:5173
- 백엔드 API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html

---

## 📁 프로젝트 구조

```
vidio-ai-agent/
├── src/main/java/com/vidioaiagent/
│   ├── config/           # CORS, Security, WebSocket 설정
│   ├── controller/       # REST API 엔드포인트
│   │   ├── AdGenerateController.java   # 프로젝트 CRUD
│   │   ├── FileController.java         # 영상/썸네일 서빙
│   │   └── TrendController.java        # 트렌드 단독 분석
│   ├── dto/              # Request/Response DTO
│   ├── entity/           # JPA 엔티티
│   ├── enums/            # Platform, Style, ProjectStatus
│   ├── repository/       # JPA Repository
│   ├── service/          # 비즈니스 로직
│   │   ├── AgentOrchestrator.java      # 🔥 파이프라인 오케스트레이터
│   │   ├── TrendAnalysisService.java   # 트렌드 분석 (Claude AI)
│   │   ├── AdCopyService.java          # 씬 스크립트 생성 (Claude AI)
│   │   ├── VideoGenerateService.java   # 🔥 영상 합성 (Pexels + FFmpeg)
│   │   ├── StockImageService.java      # Pexels API 연동
│   │   └── ThumbnailService.java       # 썸네일 생성
│   └── util/             # 프롬프트 템플릿
├── frontend/             # React 프론트엔드
│   ├── src/
│   │   ├── api/          # Axios API 클라이언트
│   │   ├── components/   # PipelineProgress, ResultPanel
│   │   ├── hooks/        # useProjectPolling
│   │   ├── pages/        # Create, Project, History
│   │   ├── store/        # Zustand 스토어
│   │   └── types/        # TypeScript 타입
│   └── vite.config.ts
├── docker-compose.yml    # Docker 배포
├── Dockerfile
└── build.gradle
```

---

## 📡 API 명세

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/projects` | 프로젝트 생성 + 파이프라인 시작 |
| `GET` | `/api/projects/{id}` | 프로젝트 상태/결과 조회 |
| `GET` | `/api/projects` | 전체 프로젝트 목록 |
| `POST` | `/api/projects/{id}/retry` | 단계 재실행 |
| `GET` | `/api/projects/options` | 플랫폼/스타일 옵션 목록 |
| `GET` | `/api/files/video/{fileName}` | 영상 파일 스트리밍 |
| `GET` | `/api/files/thumbnail/{fileName}` | 썸네일 이미지 |
| `WS` | `/ws` → `/topic/progress/{id}` | 실시간 진행상황 |

---

## 📄 라이선스

MIT License
