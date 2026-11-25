package com.learninglab.urlshortener.dto;

import java.time.Instant;

public record ShortUrlStatsResponse(String code, String shortUrl, String originalUrl, long clickCount,
		Instant createdAt, Instant lastAccessedAt) {
}
