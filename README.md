# URL Shortener

A Spring Boot REST API that shortens http/https URLs, redirects short codes to the original URL (HTTP 302) and counts clicks. Java 21, Spring Boot 4.2, PostgreSQL 18, Flyway.

## Quick start (Docker)

```bash
docker compose up --build
```

Starts PostgreSQL and the app. Once both are healthy:

```bash
curl -i -X POST http://localhost:8080/api/urls -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/some/long/path"}'
curl -i http://localhost:8080/<code>            # 302 -> original URL
curl -i http://localhost:8080/api/urls/<code>/stats
```

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

## Local development

Run only the database in Docker and the app from Maven:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Flyway applies the migrations on startup.

## Testing

Docker must be running; DB-backed tests share one throwaway PostgreSQL container (Testcontainers).

```bash
./mvnw test                                   # all tests + JaCoCo report
./mvnw test -Dtest=ShortUrlServiceTest        # one class
open target/site/jacoco/index.html            # coverage report
```

| Type | Classes | Notes |
|------|---------|-------|
| Unit (Mockito) | `ShortUrlServiceTest`, `ShortCodeGeneratorTest` | No Spring context |
| Web slice (`@WebMvcTest`) | `*ControllerTest`, `GlobalExceptionHandlerTest` | Service mocked |
| Repository (`@DataJpaTest`) | `ShortUrlRepositoryTest` | Real PostgreSQL + Flyway |
| Integration (`@SpringBootTest` + MockMvc) | `*IntegrationTest`, `UrlShortenerApplicationTests` | Full stack, shared context |

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/urlshortener` | JDBC URL (compose sets `jdbc:postgresql://postgres:5432/urlshortener`) |
| `DB_USERNAME` | `urlshortener` | Database user (also used by the compose postgres service) |
| `DB_PASSWORD` | `urlshortener` | Database password (also used by the compose postgres service) |
| `APP_BASE_URL` | `http://localhost:8080` | Public base used to build `shortUrl` |

The defaults are for local development only.

## Docs

[PROJECT_OVERVIEW](PROJECT_OVERVIEW.md) · [ARCHITECTURE](ARCHITECTURE.md) · [DATABASE_DESIGN](DATABASE_DESIGN.md) · [API_DOCUMENTATION](API_DOCUMENTATION.md) · [LEARNING_NOTES](LEARNING_NOTES.md) · [PROGRESS](PROGRESS.md) · [INTERVIEW_PREPARATION](INTERVIEW_PREPARATION.md)
