# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Java 21, Spring Boot 4.2 (Maven wrapper). Docker must be running for both the app and the tests.

```bash
docker compose up -d          # local PostgreSQL 18 (needed to run the app)
./mvnw spring-boot:run        # run the app on :8080 (Flyway migrates on startup)
./mvnw test                   # all tests (Testcontainers starts a throwaway Postgres)
./mvnw test -Dtest=ShortUrlServiceTest                 # one class
./mvnw test -Dtest=ShortUrlServiceTest#retriesWhenFirstCodeExists   # one method
```

No linter is configured. Env vars: `SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_BASE_URL` (see README).

## Architecture

Layered Spring MVC app under `com.learninglab.urlshortener`: `controller` → `service` → `repository` (Spring Data JPA) → `entity`, with `dto` as Java records. Controllers hold no business logic; mapping entity → response lives in the service.

- **Creating a link** (`POST /api/urls`): the request is validated with Bean Validation on `CreateShortUrlRequest` (http/https only, max 2048). `ShortUrlService` trims the URL, asks `ShortCodeGenerator` (7-char Base62, `SecureRandom`) for a code, retries up to 5 times if `existsByCode` is true, then throws `IllegalStateException`. `shortUrl` = `app.base-url` (trailing slash stripped) + `/` + code.
- **Redirect** (`GET /{code}`, `RedirectController`): route is restricted to `[0-9A-Za-z]{7}` (other paths 404, never reach the service). `ShortUrlService.resolveRedirect` throws `ShortUrlNotFoundException` (`@ResponseStatus` 404). Responds 302, not 301 (browsers cache 301, which would defeat click counting).
- **Click counting**: `ShortUrlService.resolveRedirect` (`@Transactional`) finds the link, then calls `ShortUrlRepository.incrementClickCount`, an atomic JPQL `UPDATE` (no read-modify-write; `clearAutomatically`). `GET /api/urls/{code}/stats` (`getStats`, read-only, same 7-char route restriction) returns `ShortUrlStatsResponse`; stats reads don't count as clicks. Tests that call the service concurrently must not be `@Transactional`.
- **Not yet implemented**: SpringDoc.
- **Errors**: `GlobalExceptionHandler` (`@RestControllerAdvice` extending `ResponseEntityExceptionHandler`) returns RFC 9457 `ProblemDetail` (`application/problem+json`, `instance` = request path) for everything, including Spring MVC's own 404/405/415. 400 validation adds an `errors[{field,message}]` list; 500 hides the exception message and only logs it. New exceptions get an `@ExceptionHandler` there, not `@ResponseStatus`.
- **Schema** is owned by Flyway (`src/main/resources/db/migration`); Hibernate only runs `ddl-auto=validate`, so any entity/schema mismatch fails startup. Never edit an applied migration; add `V<n>__description.sql` and update the entity. See `DATABASE_DESIGN.md`.
- `code` is unique at the DB level; the `existsByCode` check is an optimization. `create` uses `saveAndFlush` and retries on `DataIntegrityViolationException`; pre-check collisions and save violations share the 5-attempt limit.

## Testing notes

- Spring Boot 4 specifics: use `@MockitoBean` (not `@MockBean`); `@WebMvcTest` is in `org.springframework.boot.webmvc.test.autoconfigure`; Jackson is 3.x (`tools.jackson`), so `com.fasterxml.jackson.databind` isn't available — use `JsonPath` in tests.
- JaCoCo runs with `./mvnw test`; report at `target/site/jacoco/index.html`. All Testcontainers tests share one static Postgres container (`TestcontainersConfiguration`); keep `@SpringBootTest` tests on identical annotations (`@AutoConfigureMockMvc` + `@Import`) and avoid `@DirtiesContext` so the context is reused.
- DB-backed tests import `TestcontainersConfiguration` (`@DataJpaTest` also needs `@AutoConfigureTestDatabase(replace = NONE)`). Service tests are pure Mockito with no Spring context.
- Validation runs before the service trims, so URLs with surrounding whitespace get 400 via the API.

Endpoint contract: `API_DOCUMENTATION.md`.
