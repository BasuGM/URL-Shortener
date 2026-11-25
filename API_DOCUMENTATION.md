# API Documentation

> The error body format (currently Spring's default) will be standardized in a later milestone.

## POST /api/urls

Creates a short link for an http/https URL.

**Request** (`Content-Type: application/json`)

```json
{ "url": "https://example.com/some/long/path" }
```

| Field | Rules |
|-------|-------|
| `url` | required, not blank, max 2048 chars, must match `^https?://\S+$` (http/https only; no whitespace) |

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

The base of `shortUrl` comes from `APP_BASE_URL` (default `http://localhost:8080`). Redirecting is not implemented yet.
