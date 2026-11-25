# API Documentation

> Interactive docs: Swagger UI at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs` (generated from the code; the source of truth for the contract). This file adds examples and error details.
>
> All errors use the format described in [Errors](#errors).

## POST /api/urls

Creates a short link for an http/https URL.

**Request** (`Content-Type: application/json`)

```json
{ "url": "https://example.com/some/long/path" }
```

| Field | Rules |
|-------|-------|
| `url` | required, not blank, max 2048 chars, must match `^https?://\S+$` (http/https only, no whitespace inside). Leading/trailing whitespace is stripped before validation. |

**Response `201 Created`** with `Location: <shortUrl>`

```json
{
  "code": "aB3xY9z",
  "shortUrl": "http://localhost:8080/aB3xY9z",
  "originalUrl": "https://example.com/some/long/path",
  "createdAt": "2026-11-25T10:30:00.123456Z"
}
```

**Status codes**

| Code | When |
|------|------|
| 201  | Short link created |
| 400  | Missing/blank/too long/non-http(s) URL, or malformed JSON body |
| 405/415 | Wrong HTTP method / unsupported media type |
| 500  | Unexpected server error |

**Examples**

```bash
curl -i -X POST http://localhost:8080/api/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/some/long/path"}'

# 400: unsupported scheme
curl -i -X POST http://localhost:8080/api/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"ftp://x.com"}'
```

The base of `shortUrl` comes from `APP_BASE_URL` (default `http://localhost:8080`).

## GET /{code}

Redirects to the original URL. `code` must be exactly 7 Base62 characters (`[0-9A-Za-z]`); anything else is not routed here and returns 404. A temporary `302` is used (not `301`) so browsers don't cache it and every redirect is counted.

**Response `302 Found`** with `Location: <originalUrl>` and an empty body. Each successful redirect increments the link's click count and updates its last-accessed time (see [stats](#get-apiurlscodestats)).

**Status codes**

| Code | When |
|------|------|
| 302  | Code exists; `Location` is the original URL |
| 404  | Unknown code, or code not matching `[0-9A-Za-z]{7}` |

**Example**

```bash
curl -i http://localhost:8080/aB3xY9z
# HTTP/1.1 302
# Location: https://example.com/some/long/path
```

## GET /api/urls/{code}/stats

Returns click statistics for a short link. `code` must be exactly 7 Base62 characters; anything else returns 404. Reading stats does not count as a click.

**Response `200 OK`**

```json
{
  "code": "aB3xY9z",
  "shortUrl": "http://localhost:8080/aB3xY9z",
  "originalUrl": "https://example.com/some/long/path",
  "clickCount": 2,
  "createdAt": "2026-11-25T10:30:00.123456Z",
  "lastAccessedAt": "2026-11-25T10:35:10.654321Z"
}
```

`lastAccessedAt` is `null` until the first redirect.

**Status codes**

| Code | When |
|------|------|
| 200  | Code exists |
| 404  | Unknown code, or code not matching `[0-9A-Za-z]{7}` |

```bash
curl -i http://localhost:8080/api/urls/aB3xY9z/stats
```

## GET /api/health

Liveness check.

**Response `200 OK`**

```json
{ "status": "UP", "service": "url-shortener", "timestamp": "2026-11-25T09:00:00.123456Z" }
```

```bash
curl -i http://localhost:8080/api/health
```

## Errors

Every error is a [ProblemDetail (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457) with `Content-Type: application/problem+json`.

| Field | Meaning |
|-------|---------|
| `status` | HTTP status code |
| `title` | Status reason phrase (e.g. `Bad Request`) |
| `detail` | Human-readable explanation |
| `instance` | Request path that caused the error |
| `errors` | Validation failures only: list of `{field, message}`, sorted by field |

```json
{
  "detail": "Validation failed",
  "instance": "/api/urls",
  "status": 400,
  "title": "Bad Request",
  "errors": [{ "field": "url", "message": "must be an http or https URL" }]
}
```

| Status | Example cause | `detail` |
|--------|---------------|----------|
| 400 | Bean Validation failure on `POST /api/urls` | `Validation failed` (+ `errors`) |
| 400 | Unparseable JSON body | `Malformed JSON request body` |
| 404 | Unknown code on `GET /{code}` | `Short URL not found: <code>` |
| 404 | Any path with no route (e.g. `/abc`) | Spring's message |
| 405 | Wrong method (e.g. `POST /api/health`) | Spring's message |
| 415 | Non-JSON `Content-Type` on `POST /api/urls` | Spring's message |
| 500 | Any unexpected failure | `An unexpected error occurred` (details are only logged server-side) |
