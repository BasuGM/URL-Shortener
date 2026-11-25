package com.learninglab.urlshortener.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learninglab.urlshortener.dto.HealthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health", description = "Liveness check")
@RestController
@RequestMapping("/api/health")
public class HealthController {

	@Operation(summary = "Service health")
	@GetMapping
	public HealthResponse health() {
		return new HealthResponse("UP", "url-shortener", Instant.now());
	}
}
