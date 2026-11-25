# Database Design

PostgreSQL 18. The schema is managed by Flyway; Hibernate only validates it (`spring.jpa.hibernate.ddl-auto=validate`).

## Table `short_urls`

| Column         | Type            | Constraints                                  | Meaning                                  |
|----------------|-----------------|----------------------------------------------|------------------------------------------|
| `id`           | `BIGINT`        | `GENERATED ALWAYS AS IDENTITY`, primary key  | Surrogate key, assigned by the database  |
| `code`         | `VARCHAR(16)`   | `NOT NULL`, unique (`uk_short_urls_code`)    | The short code that identifies a URL     |
| `original_url` | `VARCHAR(2048)` | `NOT NULL`                                   | The full URL the short code points to    |
| `created_at`   | `TIMESTAMPTZ`   | `NOT NULL`                                   | When the record was created (UTC instant) |

## Why these constraints

- **Unique `code`**: a code must resolve to exactly one URL. Enforcing it in the database guarantees this even under concurrent inserts; the unique constraint also creates the index used for lookups by code.
- **`code` limited to 16 characters**: short codes are meant to be short; the limit bounds index size and rejects malformed values.
- **`original_url` limited to 2048 characters**: a common practical maximum URL length for browsers and servers, so it bounds row size.
- **`NOT NULL` everywhere**: every record needs a code, a target and a creation time.
- **`GENERATED ALWAYS` identity**: the application can never supply its own id, so ids stay consistent.
- **`TIMESTAMPTZ`**: stores an unambiguous point in time, which maps to `java.time.Instant`.

## Schema changes

All schema changes go through new Flyway migrations in `src/main/resources/db/migration` (`V2__description.sql`, and so on). Never edit a migration that has already been applied. Update the JPA entity to match, since `validate` fails startup on any mismatch.
