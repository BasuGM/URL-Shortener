package com.learninglab.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
		@Schema(example = "https://example.com/some/long/path")
		@NotBlank
		@Size(max = 2048)
		@Pattern(regexp = "^https?://\\S+$", message = "must be an http or https URL")
		String url) {

	public CreateShortUrlRequest {
		url = url == null ? null : url.strip();
	}
}
