# ConvertLab Agent Guide

Read this first before changing code in this workspace. ConvertLab is split into two separate codebases:

- Frontend: `convertlab-frontend/convert-lab`
- Backend: `convertlab-backend`

The goal of this file is to reduce repo-search tokens. Use it as the project map, then inspect only the files needed for the requested change.

## Product Snapshot

ConvertLab is a web app for document and image utilities:

- PDF tools: merge, split, extract pages, image-to-PDF, compress PDF, edit PDF with overlays, add/remove PDF password.
- Image tools: compress images and crop images.
- Auth: email/password signup with OTP, login, refresh/logout, Google login.
- Analytics/contact: page visit tracking and contact inquiry submission.
- DocMind: upload a PDF, ingest text/chunks/embeddings, and ask RAG questions with WebSocket progress/events.

## Tech Stack

Frontend:

- Angular 20 app with standalone/lazy components.
- Angular Material/CDK, SCSS, RxJS.
- Zoneless change detection via `provideZonelessChangeDetection()`.
- HTTP uses `provideHttpClient(withFetch(), withInterceptors(...))`.
- WebSocket uses STOMP over SockJS.
- PDF previews use `pdfjs-dist`; image conversion includes `heic2any`.

Backend:

- Java 21, Spring Boot 4.0.0, Maven wrapper.
- Spring Web, Security, JPA/JDBC, Flyway, WebSocket, Mail, Actuator, WebFlux.
- PostgreSQL with pgvector.
- PDFBox for PDF operations, Tess4J for OCR.
- AWS SDK S3 plus local storage implementation.
- OpenAI-backed chat/embedding providers.
- Log4j2 logging.

## Commands

Frontend commands, run from `convertlab-frontend/convert-lab`:

- Install: `npm install`
- Dev server: `npm start` or `npx ng serve`
- Build: `npm run build`
- Test: `npm test`
- SSR build output runner: `npm run serve:ssr:convert-lab`

Backend commands, run from `convertlab-backend`:

- Build/test: `./mvnw test`
- Package: `./mvnw package`
- Run app: `./mvnw spring-boot:run`
- Docker stack: `docker compose up --build`

Notes:

- Frontend dev API URL is `http://localhost:8080/api`.
- Backend default port is `8080`, context path is `/api`.
- Docker compose maps backend host port to `${HOST_PORT:-8081}` and runs Postgres/pgvector.

## Frontend Map

Important paths:

- `src/app/app.routes.ts` - public routes and lazy component loading.
- `src/app/app.config.ts` - Angular providers, HTTP interceptors, auth initialization.
- `src/environments/environment.ts` - local API URL and Google client id.
- `src/environments/environment.prod.ts` - production API URL (`/api`).
- `src/styles.scss` - global styles.
- `src/styles/variables.scss` - SCSS color tokens.
- `src/styles/material-overrides.scss` - Angular Material overrides.
- `src/styles/util.scss` - shared utility styles.
- `public/robots.txt`, `public/sitemap.xml`, `routes.txt` - SEO/prerender assets.
- `scripts/generate-sitemap.js`, `scripts/set-version.js` - build-time helpers.

Feature/component areas:

- `src/app/components/home` - landing/home.
- `src/app/components/authentication` - login/signup/Google sign-in.
- `src/app/components/pdf` - PDF tool pages.
- `src/app/components/image` - image tool pages and image crop editor.
- `src/app/components/shared` - reusable UI: file uploader, thumbnails, navbar, search, buttons, skeletons, page range input.
- `src/app/components/layout` - app shell/layout.
- `src/app/doc-mind` - DocMind RAG experience.
- `src/app/seo` - meta tags and structured data.
- `src/app/models` - request/response models for conversion tools.
- `src/app/services` - API, auth, uploads, validation, WebSocket, thumbnails, snackbar, version.
- `src/app/interceptors` - auth/session/error/blob handling.
- `src/app/guards/auth.guard.ts` - auth/guest route guards.

Routes currently defined:

- `/`
- `/signup`
- `/login`
- `/about`
- `/contact`
- `/contact-me` redirects to `/contact`
- `/merge-pdf`
- `/extract-pdf`
- `/split-pdf`
- `/image-to-pdf`
- `/compress-pdf`
- `/edit-pdf`
- `/compress-image`
- `/pdf-password`
- `/crop-image`
- `/doc-mind` redirects to `/docmind`
- `/docmind`

Frontend conventions:

- Components are standalone-style Angular components with `.component.ts`, `.component.html`, `.component.scss`.
- Add new pages by adding a lazy route in `src/app/app.routes.ts` and placing the feature under the relevant `components/...` folder, or under `doc-mind` for DocMind-specific UI.
- API service classes usually inject `HttpService` and build URLs from `environment.apiUrl`.
- Use `HttpService` for normal API calls so `withCredentials: true` and shared options are consistent.
- Blob download endpoints should set `responseType: 'blob'`, `observe: 'response'`, and `IS_BLOB_REQUEST` via `HttpContext`.
- Auth and refresh flows use `AuthService`, `AuthStateService`, `AuthInitService`, `auth.interceptor.ts`, and `session.interceptor.ts`.
- Global errors are handled by `error.interceptor.ts`; blob API errors by `blob-error.interceptor.ts`.
- Use `SnackbarService` for user-facing notifications.
- File uploads should go through `FileUploadService`; client validation through `FileValidationService`.
- Keep SEO updates near `src/app/seo/seo.config.ts` and route/component initialization.

DocMind frontend:

- Main page: `src/app/doc-mind/doc-mind.component.*`
- API service: `src/app/doc-mind/services/document-rag.service.ts`
- Models: `src/app/doc-mind/models/docmind.models.ts`
- UI: `components/doc-panel`, `components/chat-panel`
- Live events come from `WebSocketService`; frontend subscribes to `/topic/session/{sessionId}` and `/user/queue/events` when authenticated.

## Backend Map

Main package: `src/main/java/com/convertlab/convertlab_backend`

Important paths:

- `ConvertlabBackendApplication.java` - Spring Boot entry point.
- `src/main/resources/application.yml` - base config.
- `src/main/resources/application-prod.yml` - production S3 config.
- `src/main/resources/db/migration` - Flyway migrations.
- `.env.example` - sample required environment variables.
- `docker-compose.yml` - Postgres/pgvector plus backend.
- `pom.xml` - Maven dependencies and Java version.

Backend package roles:

- `service_web/controllers` - REST controllers and DTOs.
- `service_core` - core PDF/image/contact/analytics/user business services.
- `service_ai` - DocMind/RAG, OpenAI chat/embeddings, chunking, AI rate limits.
- `service_storage` - storage abstraction and local/S3 implementations.
- `service_email` - OTP/contact email and SMTP/mock senders.
- `service_util` - PDF/image/file/email/IP/geo utility helpers.
- `authentication` - signup/login/refresh token/Google OAuth services.
- `config` - Spring Security, CORS, JWT filter, WebSocket, S3, validation, proxy/request context.
- `entity` - JPA entities.
- `repository` - Spring Data repositories.
- `exception` - app exceptions and `GlobalExceptionHandler`.
- `ratelimit` - token bucket and request rate-limiting filter.
- `websocket` - event model and server-side WebSocket sender.
- `api` and `api/enums` - common response wrapper and enums.

REST endpoints are under backend context path `/api`:

- Auth: `/auth/signup`, `/auth/verify-otp`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/google`
- Upload: `/upload/pdf`, `/upload/image`
- PDF: `/pdf/extract`, `/pdf/thumbnail/{assetId}`, `/pdf/merge`, `/pdf/split`, `/pdf/images-to-pdf`, `/pdf/compress`, `/pdf/edit`, `/pdf/password`
- Image: `/image/compress`, `/image/crop`
- Documents/RAG: `/documents/ingest`, `/documents/query`
- Analytics: `/analytics/page-visit`
- Contact: `/contact/inquiries`
- User: `DELETE /user`
- Version: `/version`

Backend conventions:

- Controllers should stay thin and delegate to services.
- JSON API responses should use `ApiResponse.success(data)` or `ApiResponse.failure(message, code)`.
- Binary endpoints return `ResponseEntity<byte[]>`/resources as already established in controllers.
- Add new validation failures as specific exceptions where useful, then map them in `GlobalExceptionHandler`.
- Database changes should use a new Flyway migration in `src/main/resources/db/migration`; do not rely on Hibernate DDL (`ddl-auto: none`).
- Add JPA entities in `entity` and repositories in `repository`.
- Prefer constructor injection with Lombok `@RequiredArgsConstructor`, matching current backend style.
- Use Log4j2 (`@Log4j2`) instead of `System.out` for production logging.
- Security is stateless; JWT and rate limiting filters are wired in `SecurityConfig`.
- Most endpoints are currently permitted by `SecurityConfig`; auth endpoints are explicitly public and `/documents/**` auth is commented as temporarily allowed.
- WebSocket endpoint is `/ws` with SockJS; broker destinations are `/topic`, `/queue`, `/user`.

Data/storage:

- Postgres migrations include page visits, users/OTP, refresh tokens, pgvector, document chunks/embeddings, AI usage, auth schema refactor, and contact inquiries.
- Storage has both local and S3 strategies; inspect `service_storage/impl` before changing upload/download behavior.
- Temporary file settings are in `application.yml` under `temp.*`.

AI/RAG:

- RAG orchestration: `service_ai/RagService.java`
- Text extraction: `PdfTextExtractionService.java`
- Chunking: `DocumentChunker.java` and `impl/DefaultDocumentChunker.java`
- Embeddings: `EmbeddingService.java`, `EmbeddingProvider.java`, `impl/OpenAiEmbeddingProvider.java`
- Chat: `ChatService.java`, `impl/OpenAiChatService.java`
- Usage/rate limit: `UserAiUsageService.java`, `config/AiRateLimitConfig.java`

## Configuration

Common backend env vars:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_SECRET`
- `GEO_LOCATION_API_URL`
- `GEO_LOCATION_API_KEY`
- `OPENAI_API_KEY`
- `OPENAI_API_BASE_URL`
- `GOOGLE_CLIENT_ID`
- `AWS_S3_BUCKET_NAME`
- `AWS_S3_REGION`

Config defaults/notes:

- `SERVER_CONTEXT_PATH` defaults to `/api`.
- `COOKIE_SECURE` defaults to `true`; local development may need a different value depending on browser/cookie testing.
- Validation limits live under `validation.pdf` and `validation.image`.
- AI daily and per-minute limits live under `ai.rate-limit`.

## Verification Checklist

Choose the smallest relevant checks:

- Frontend-only change: run `npm run build` from `convertlab-frontend/convert-lab`; run `npm test` when logic is changed and tests are available.
- Backend-only change: run `./mvnw test` from `convertlab-backend`.
- Full-stack API contract change: run frontend build and backend tests, and manually compare the frontend service request/response model with the backend controller DTO.
- UI changes: inspect the changed route in a browser if a dev server is available.

## Token-Saving Workflow For Future Codex Runs

1. Read this `AGENTS.md` first.
2. Identify whether the task is frontend, backend, or full-stack.
3. Inspect only the relevant files listed above.
4. For endpoint changes, check both the Angular service/model and the Spring controller/DTO.
5. For DB changes, check entity, repository, service, and Flyway migration together.
6. For UI changes, check the component `.ts`, `.html`, `.scss`, plus shared components/styles it imports.
7. Keep this file updated when adding major routes, services, endpoints, migrations, or conventions.

## Known Local Notes

- Existing untracked/modified files may be user-owned; do not clean or revert them without explicit permission.
- Current observed dirty state included frontend `package-lock.json`, `.DS_Store` files, backend `env`, `env.dev`, and `ec2-deploy-local-image.sh`.
- `convertlab-backend/src/main/java/com/convertlab/convertlab_backend/service_core/impl/asdf.json` exists and looks suspicious by name; inspect before relying on it.
- `MockEmailSender` intentionally prints mock email/OTP details.
- `S3StorageService` has a TODO about replacing a byte-array approach with streaming.
