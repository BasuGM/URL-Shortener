package com.learninglab.urlshortener.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learninglab.urlshortener.dto.HealthResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	@GetMapping
	public HealthResponse health() {
		return new HealthResponse("UP", "url-shortener", Instant.now());
	}
}
