# Project Overview

A learning project: a small but production-shaped URL shortener built with Spring Boot.

## Features

- `POST /api/urls`: create a short link (7-char Base62 code) for an http/https URL.
- `GET /{code}`: 302 redirect to the original URL; counts the click.
- `GET /api/urls/{code}/stats`: click count, created and last-accessed timestamps.
- `GET /api/health`: liveness check.
- RFC 9457 `ProblemDetail` errors everywhere; OpenAPI docs at `/swagger-ui.html`.

## Stack

Java 21 · Spring Boot 4.2 (MVC, Validation, Data JPA) · PostgreSQL 18 · Flyway · springdoc-openapi 3.2.1 · JUnit 5, Mockito, Testcontainers, JaCoCo · Docker (multi-stage image, compose).

## Package layout (`com.learninglab.urlshortener`)

| Package | Contents |
|---------|----------|
| `controller` | `ShortUrlController`, `RedirectController`, `HealthController`; HTTP only |
| `service` | `ShortUrlService` (business logic), `ShortCodeGenerator` |
| `repository` | `ShortUrlRepository` (Spring Data JPA, atomic click increment) |
| `entity` | `ShortUrl` |
| `dto` | Java records for requests/responses |
| `exception` | `ShortUrlNotFoundException`, `GlobalExceptionHandler` |
| `config` | `OpenApiConfig` |

Run it with the [README](README.md); rules and flows are in [ARCHITECTURE.md](ARCHITECTURE.md).
