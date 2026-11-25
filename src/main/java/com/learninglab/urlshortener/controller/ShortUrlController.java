package com.learninglab.urlshortener.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learninglab.urlshortener.dto.CreateShortUrlRequest;
import com.learninglab.urlshortener.dto.ShortUrlResponse;
import com.learninglab.urlshortener.dto.ShortUrlStatsResponse;
import com.learninglab.urlshortener.service.ShortUrlService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/urls")
public class ShortUrlController {

	private final ShortUrlService service;

	public ShortUrlController(ShortUrlService service) {
		this.service = service;
	}

	@GetMapping("/{code:[0-9A-Za-z]{7}}/stats")
	public ShortUrlStatsResponse stats(@PathVariable String code) {
		return service.getStats(code);
	}

	@PostMapping
	public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
		ShortUrlResponse response = service.create(request);
		return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
	}
}
