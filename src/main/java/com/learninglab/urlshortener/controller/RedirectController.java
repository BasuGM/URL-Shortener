package com.learninglab.urlshortener.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.learninglab.urlshortener.service.ShortUrlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Redirect", description = "Follow a short link")
@RestController
public class RedirectController {

	private final ShortUrlService service;

	public RedirectController(ShortUrlService service) {
		this.service = service;
	}

	// 302, not 301: browsers cache 301s, which would bypass click counting later.
	@Operation(summary = "Redirect to the original URL and count the click")
	@ApiResponse(responseCode = "302", description = "Redirect to the original URL (Location header)")
	@ApiResponse(responseCode = "404", description = "Unknown code")
	@GetMapping("/{code:[0-9A-Za-z]{7}}")
	public ResponseEntity<Void> redirect(@PathVariable String code) {
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(service.resolveRedirect(code)))
				.build();
	}
}
