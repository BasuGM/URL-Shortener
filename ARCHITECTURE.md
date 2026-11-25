# Architecture

## Layers

`controller` → `service` → `repository` → PostgreSQL. DTOs are records at the edges; the entity never leaves the service. Controllers hold no business logic; entity → response mapping lives in `ShortUrlService`.

## Request flows

**Create** (`POST /api/urls`)
1. Bean Validation on `CreateShortUrlRequest` (not blank, ≤ 2048, `^https?://\S+$`); failures → 400.
2. `ShortUrlService.create` generates a 7-char Base62 code (`ShortCodeGenerator`, `SecureRandom`).
3. If `existsByCode` is true, try another code; otherwise `saveAndFlush`. A `DataIntegrityViolationException` (lost race on the unique `code`) also triggers a retry. Pre-check collisions and save violations share one limit of 5 attempts, then `IllegalStateException` → 500.
4. Response 201 with `Location: shortUrl` (`app.base-url` without trailing slash + `/` + code).

**Redirect** (`GET /{code}`)
1. The route is `/{code:[0-9A-Za-z]{7}}`; anything else is a 404 that never reaches the service. This also keeps `/swagger-ui.html`, `/v3/api-docs` and `/api/...` from matching (they don't fit 7 alphanumerics).
2. `resolveRedirect` (`@Transactional`) loads the link, runs the atomic `incrementClickCount` UPDATE, returns the URL.
3. Response 302 + `Location`. 302, not 301: browsers cache 301s, which would bypass counting.

**Stats** (`GET /api/urls/{code}/stats`): `getStats` (`@Transactional(readOnly = true)`) maps the entity to `ShortUrlStatsResponse`. Reading stats is not a click.

## Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice` extending `ResponseEntityExceptionHandler`) returns `application/problem+json` with `instance` = request path for everything, including Spring's own 404/405/415. 400 validation adds `errors[{field,message}]`. 500 hides the exception message and only logs it. New exceptions get an `@ExceptionHandler` there.

## Transactions

- `create` is deliberately **not** `@Transactional`. A failed INSERT aborts the surrounding Postgres transaction, so the retry after `DataIntegrityViolationException` would fail too. Without a service-level transaction, each `existsByCode`/`saveAndFlush` runs in its own repository transaction and a retry starts clean.
- `resolveRedirect` is one transaction: find + increment commit together.
- The click increment is a single `UPDATE ... click_count = click_count + 1`, so concurrent redirects can't lose counts (no read-modify-write). The query uses `clearAutomatically` so the persistence context doesn't serve stale rows.
- `spring.jpa.open-in-view=false`: no session leaks into the web layer.

## Schema

Owned by Flyway (`db/migration`); Hibernate only validates. See [DATABASE_DESIGN.md](DATABASE_DESIGN.md).

## Deployment

Multi-stage `Dockerfile` (JDK build with cached dependency layer → JRE runtime, non-root user). `docker-compose.yml` runs `postgres` and `app`; `app` waits for the postgres healthcheck.
