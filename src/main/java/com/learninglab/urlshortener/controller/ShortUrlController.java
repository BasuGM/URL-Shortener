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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Short URLs", description = "Create short links and read their statistics")
@RestController
@RequestMapping("/api/urls")
public class ShortUrlController {

	private final ShortUrlService service;

	public ShortUrlController(ShortUrlService service) {
		this.service = service;
	}

	@Operation(summary = "Get click statistics for a short link")
	@ApiResponse(responseCode = "404", description = "Unknown code")
	@GetMapping("/{code:[0-9A-Za-z]{7}}/stats")
	public ShortUrlStatsResponse stats(@PathVariable String code) {
		return service.getStats(code);
	}

	@Operation(summary = "Create a short link")
	@ApiResponse(responseCode = "201", description = "Short link created")
	@ApiResponse(responseCode = "400", description = "Invalid or malformed request")
	@PostMapping
	public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
		ShortUrlResponse response = service.create(request);
		return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
	}
}
