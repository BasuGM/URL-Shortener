# Learning Notes (Spring Boot, with Node/Express comparisons)

| # | Milestone | Key concepts | Node/Express analogue |
|---|-----------|--------------|-----------------------|
| 1 | Setup + health endpoint | `@RestController`, `@GetMapping`, records as DTOs, Maven wrapper | `app.get('/health', ...)`; `package.json` + `npx` ≈ `pom.xml` + `./mvnw` |
| 2 | PostgreSQL + Flyway + JPA | Entity, Spring Data repository, Flyway migrations, `ddl-auto=validate` | Prisma/Knex migrations, ORM models; here the DB schema is the source of truth and the ORM only checks it |
| 3 | `POST /api/urls` | Bean Validation (`@Valid`, `@NotBlank`, `@Pattern`), constructor injection, collision retry | `express-validator`/zod middleware; DI is automatic instead of manual `require` |
| 4 | Redirect | Regex path variables, 302 vs 301, `ResponseEntity` | `res.redirect(302, url)`; route regex `/:code([0-9A-Za-z]{7})` |
| 5 | Error handling | `@RestControllerAdvice`, RFC 9457 `ProblemDetail` | A central `app.use((err, req, res, next) => ...)` error middleware |
| 6 | Click counting + stats | `@Transactional`, atomic `UPDATE`, concurrent tests | `UPDATE ... SET n = n + 1` via `db.query`; Node's single thread hides races inside one process, but not across processes or in the DB; the JVM is truly multi-threaded |
| 7 | Test gaps | Testcontainers, shared container/context, JaCoCo, unique-constraint race handling | `supertest` + a docker test DB; `nyc`/`c8` for coverage |
| 8 | OpenAPI + Docker | springdoc, multi-stage Dockerfile, compose healthchecks | `swagger-jsdoc`; `node:alpine` build then slim runtime image |

## Concepts worth remembering

- **Transactions can break retries.** A failed statement aborts a Postgres transaction. `create` must not be `@Transactional` or the retry after a unique violation fails. Node equivalent: reusing one `BEGIN` block after an error.
- **The DB enforces uniqueness; the app only optimizes.** `existsByCode` avoids most collisions, the unique constraint is the real guard.
- **Spring proxies.** `@Transactional` works through a proxy, so calling a transactional method from inside the same class bypasses it. No Express equivalent.
- **Spring Boot 4 specifics.** `@MockitoBean`, Jackson 3 (`tools.jackson`), `@WebMvcTest` under `org.springframework.boot.webmvc.test.autoconfigure`.
- **Config via env.** `${DB_URL:default}` in `application.properties` ≈ `process.env.DB_URL || default`.
- **Test contexts are cached.** Keep `@SpringBootTest` annotations identical across classes so the context (and container) is reused.
