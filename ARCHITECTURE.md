# Architecture

Diagrams of the layers and of the create and redirect flows are in the [README](README.md#architecture). This file records the rules behind them.

## Layers

`controller` → `service` → `repository` → PostgreSQL. DTOs are records at the edges; the `ShortUrl` entity never leaves the service. Controllers hold no business logic; entity → response mapping lives in `ShortUrlService`.

## Request flows (summary)

- **Create** (`POST /api/urls`): Bean Validation on `CreateShortUrlRequest` (not blank, ≤ 2048, `^https?://\S+$`; the record strips surrounding whitespace first). `ShortUrlService.create` generates a 7-char Base62 code (`ShortCodeGenerator`, `SecureRandom`) and skips it if `existsByCode`; otherwise `saveAndFlush`. A `DataIntegrityViolationException` (lost race on the unique `code`) also retries. Both cases share 5 attempts, then `IllegalStateException` → 500. Response is 201 with `Location: shortUrl`, where `shortUrl` = `app.base-url` (trailing slash stripped) + `/` + code.
- **Redirect** (`GET /{code}`): route `/{code:[0-9A-Za-z]{7}}`; other paths 404 without reaching the service. This also keeps `/swagger-ui.html`, `/v3/api-docs` and `/api/...` from matching. `resolveRedirect` finds the link, runs the atomic increment, returns the URL; the controller answers 302. 302, not 301: browsers cache 301s, which would bypass counting.
- **Stats** (`GET /api/urls/{code}/stats`): `getStats` (`@Transactional(readOnly = true)`) maps the entity to `ShortUrlStatsResponse`. Reading stats is not a click.

## Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice` extending `ResponseEntityExceptionHandler`) returns `application/problem+json` with `instance` = request path for everything, including Spring's own 404/405/415. 400 validation adds `errors[{field,message}]`. 500 hides the exception message and only logs it. New exceptions get an `@ExceptionHandler` there. Status table: [API_DOCUMENTATION.md](API_DOCUMENTATION.md#errors).

## Transactions

- `create` is deliberately **not** `@Transactional`. A failed INSERT aborts the surrounding Postgres transaction, so a retry after `DataIntegrityViolationException` would fail too. Without a service-level transaction, each `existsByCode`/`saveAndFlush` runs in its own repository transaction and a retry starts clean.
- `resolveRedirect` is one `@Transactional` unit: find + increment commit together.
- The click increment is a single `UPDATE ... click_count = click_count + 1`, so concurrent redirects can't lose counts. The query uses `clearAutomatically` so the persistence context doesn't serve stale rows.
- `spring.jpa.open-in-view=false`: no JPA session leaks into the web layer.

## Schema

Owned by Flyway (`db/migration`); Hibernate only validates. See [DATABASE_DESIGN.md](DATABASE_DESIGN.md).

## Deployment

Multi-stage `Dockerfile` (JDK build with a cached dependency layer → JRE runtime, non-root user). `docker-compose.yml` runs `postgres` and `app`; `app` waits for the postgres healthcheck. See the [README](README.md#quick-start).
