package com.learninglab.urlshortener.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.learninglab.urlshortener.service.ShortUrlService;

@RestController
public class RedirectController {

	private final ShortUrlService service;

	public RedirectController(ShortUrlService service) {
		this.service = service;
	}

	// 302, not 301: browsers cache 301s, which would bypass click counting later.
	@GetMapping("/{code:[0-9A-Za-z]{7}}")
	public ResponseEntity<Void> redirect(@PathVariable String code) {
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(service.resolveRedirect(code)))
				.build();
	}
}
