# URL Shortener

A Spring Boot REST API for shortening URLs. So far: project setup, a health endpoint and PostgreSQL persistence.

## Prerequisites

- Java 21
- Docker (for the local PostgreSQL database and for the tests, which use Testcontainers)

## Run

Start the database, then the app:

```bash
docker compose up -d
./mvnw spring-boot:run
```

The server listens on port 8080 (override with `SERVER_PORT`). Flyway applies the schema migrations on startup.

### Environment variables

| Variable       | Default                                     | Purpose                                    |
|----------------|---------------------------------------------|--------------------------------------------|
| `SERVER_PORT`  | `8080`                                      | HTTP port                                  |
| `DB_URL`       | `jdbc:postgresql://localhost:5432/urlshortener` | JDBC URL used by the app               |
| `APP_BASE_URL` | `http://localhost:8080`                     | Public base URL used to build short links  |
| `DB_USERNAME`  | `urlshortener`                              | Database user (also used by docker compose) |
| `DB_PASSWORD`  | `urlshortener`                              | Database password (also used by docker compose) |

The defaults are for local development only.

## Testing

Docker must be running; DB-backed tests start one shared throwaway PostgreSQL container (Testcontainers).

| Type | Classes | Notes |
|------|---------|-------|
| Unit (Mockito) | `ShortUrlServiceTest`, `ShortCodeGeneratorTest` | No Spring context |
| Web slice (`@WebMvcTest`) | `*ControllerTest`, `GlobalExceptionHandlerTest` | Service mocked |
| Repository (`@DataJpaTest`) | `ShortUrlRepositoryTest` | Real PostgreSQL + Flyway |
| Integration (`@SpringBootTest` + MockMvc) | `*IntegrationTest`, `UrlShortenerApplicationTests` | Full stack, one shared context |

```bash
./mvnw test                                   # all tests + JaCoCo report
./mvnw test -Dtest=ShortUrlServiceTest        # one class
open target/site/jacoco/index.html            # coverage report (after a test run)
```

## Try it

```bash
curl -i http://localhost:8080/api/health
```

```json
{"status":"UP","service":"url-shortener","timestamp":"2026-11-25T09:00:00.123456Z"}
```

See [DATABASE_DESIGN.md](DATABASE_DESIGN.md) for the schema.
