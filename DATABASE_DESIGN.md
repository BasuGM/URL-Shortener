# Database Design

PostgreSQL 18. The schema is managed by Flyway; Hibernate only validates it (`spring.jpa.hibernate.ddl-auto=validate`).

## Table `short_urls`

| Column         | Type            | Constraints                                  | Meaning                                  |
|----------------|-----------------|----------------------------------------------|------------------------------------------|
| `id`           | `BIGINT`        | `GENERATED ALWAYS AS IDENTITY`, primary key  | Surrogate key, assigned by the database  |
| `code`         | `VARCHAR(16)`   | `NOT NULL`, unique (`uk_short_urls_code`)    | The short code that identifies a URL     |
| `original_url` | `VARCHAR(2048)` | `NOT NULL`                                   | The full URL the short code points to    |
| `created_at`   | `TIMESTAMPTZ`   | `NOT NULL`                                   | When the record was created (UTC instant) |
| `click_count`  | `BIGINT`        | `NOT NULL`, default `0` (added in V2)        | Number of redirects served for this code |
| `last_accessed_at` | `TIMESTAMPTZ` | nullable (added in V2)                      | Time of the latest redirect; `NULL` until first click |

## Why these constraints

- **Unique `code`**: a code must resolve to exactly one URL. Enforcing it in the database guarantees this even under concurrent inserts; the unique constraint also creates the index used for lookups by code.
- **`code` limited to 16 characters**: short codes are meant to be short; the limit bounds index size and rejects malformed values.
- **`original_url` limited to 2048 characters**: a common practical maximum URL length for browsers and servers, so it bounds row size.
- **`NOT NULL` everywhere**: every record needs a code, a target and a creation time.
- **`GENERATED ALWAYS` identity**: the application can never supply its own id, so ids stay consistent.
- **`TIMESTAMPTZ`**: stores an unambiguous point in time, which maps to `java.time.Instant`.

## Click counting (V2)

`V2__add_click_count.sql` adds `click_count` (`NOT NULL DEFAULT 0`, so existing rows start at 0) and `last_accessed_at` (nullable, because a never-clicked link has no access time).

The increment is a single atomic `UPDATE short_urls SET click_count = click_count + 1, last_accessed_at = ? WHERE code = ?` (`ShortUrlRepository.incrementClickCount`), not a read-modify-write in Java. With read-modify-write, two concurrent redirects can both read `5` and both write `6`, losing a click. The atomic UPDATE takes a row lock, so the database serializes concurrent increments and none are lost. A test with 20 simultaneous redirects verifies this.

## Running in Docker

Compose's `postgres` service creates database `urlshortener`; the `app` container connects to host `postgres` and Flyway migrates on startup. Data lives in the `postgres-data` volume (`docker compose down -v` wipes it).

## Schema changes

All schema changes go through new Flyway migrations in `src/main/resources/db/migration` (`V2__description.sql`, and so on). Never edit a migration that has already been applied. Update the JPA entity to match, since `validate` fails startup on any mismatch.
