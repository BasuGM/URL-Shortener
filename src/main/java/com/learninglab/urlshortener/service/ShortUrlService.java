package com.learninglab.urlshortener.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learninglab.urlshortener.dto.CreateShortUrlRequest;
import com.learninglab.urlshortener.dto.ShortUrlResponse;
import com.learninglab.urlshortener.dto.ShortUrlStatsResponse;
import com.learninglab.urlshortener.entity.ShortUrl;
import com.learninglab.urlshortener.exception.ShortUrlNotFoundException;
import com.learninglab.urlshortener.repository.ShortUrlRepository;

@Service
public class ShortUrlService {

	private static final int MAX_ATTEMPTS = 5;

	private final ShortUrlRepository repository;
	private final ShortCodeGenerator generator;
	private final String baseUrl;

	public ShortUrlService(ShortUrlRepository repository, ShortCodeGenerator generator,
			@Value("${app.base-url}") String baseUrl) {
		this.repository = repository;
		this.generator = generator;
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	public ShortUrlResponse create(CreateShortUrlRequest request) {
		String url = request.url();
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String code = generator.generate();
			if (!repository.existsByCode(code)) {
				return toResponse(repository.save(new ShortUrl(code, url)));
			}
		}
		throw new IllegalStateException("Could not generate a unique short code");
	}

	@Transactional
	public String resolveRedirect(String code) {
		ShortUrl entity = repository.findByCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
		repository.incrementClickCount(code, Instant.now());
		return entity.getOriginalUrl();
	}

	@Transactional(readOnly = true)
	public ShortUrlStatsResponse getStats(String code) {
		ShortUrl entity = repository.findByCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
		return new ShortUrlStatsResponse(entity.getCode(), baseUrl + "/" + entity.getCode(),
				entity.getOriginalUrl(), entity.getClickCount(), entity.getCreatedAt(), entity.getLastAccessedAt());
	}

	private ShortUrlResponse toResponse(ShortUrl entity) {
		return new ShortUrlResponse(entity.getCode(), baseUrl + "/" + entity.getCode(),
				entity.getOriginalUrl(), entity.getCreatedAt());
	}
}
