# Progress

All 8 milestones are complete (59 tests passing).

| # | Milestone | Status |
|---|-----------|--------|
| 1 | Project setup, health endpoint | Done |
| 2 | PostgreSQL, Flyway, JPA entity | Done |
| 3 | `POST /api/urls` create short link | Done |
| 4 | `GET /{code}` redirect (302) | Done |
| 5 | Centralized ProblemDetail error handling | Done |
| 6 | Click counting + `GET /api/urls/{code}/stats` | Done |
| 7 | Test gaps: race-safe creation, JaCoCo, shared test container | Done |
| 8 | SpringDoc, Dockerfile, compose app service, docs | Done |

## Optional extensions

- **Link expiry**: `expires_at` column (new Flyway migration), 404/410 after expiry.
- **Custom alias**: optional `alias` in the create request; validate against the code pattern and the unique constraint (409 on conflict). Note the redirect route currently only accepts 7-char codes.
- **Rate limiting**: per-IP limit on `POST /api/urls` (e.g. a servlet filter with a token bucket, or Bucket4j), returning 429.
