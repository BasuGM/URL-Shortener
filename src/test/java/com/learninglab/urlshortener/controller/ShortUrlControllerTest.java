package com.learninglab.urlshortener.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.learninglab.urlshortener.dto.CreateShortUrlRequest;
import com.learninglab.urlshortener.dto.ShortUrlResponse;
import com.learninglab.urlshortener.service.ShortUrlService;

@WebMvcTest(ShortUrlController.class)
class ShortUrlControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortUrlService service;

	@Test
	void validRequestReturns201() throws Exception {
		when(service.create(any(CreateShortUrlRequest.class))).thenReturn(new ShortUrlResponse(
				"abc1234", "http://localhost:8080/abc1234", "https://example.com", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost:8080/abc1234"))
				.andExpect(jsonPath("$.code").value("abc1234"))
				.andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc1234"))
				.andExpect(jsonPath("$.originalUrl").value("https://example.com"))
				.andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"));
	}

	@Test
	void surroundingWhitespaceIsStrippedBeforeReachingService() throws Exception {
		when(service.create(any(CreateShortUrlRequest.class))).thenReturn(new ShortUrlResponse(
				"abc1234", "http://localhost:8080/abc1234", "https://example.com/a", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"  https://example.com/a  \"}"))
				.andExpect(status().isCreated());

		ArgumentCaptor<CreateShortUrlRequest> captor = ArgumentCaptor.forClass(CreateShortUrlRequest.class);
		verify(service).create(captor.capture());
		assertThat(captor.getValue().url()).isEqualTo("https://example.com/a");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{}",
			"{\"url\":\"\"}",
			"{\"url\":\"   \"}",
			"{\"url\":\" \\t \"}",
			"{\"url\":\"ftp://x.com\"}",
			"{\"url\":\"javascript:alert(1)\"}",
			"{\"url\":",
			"not json"
	})
	void invalidBodyReturns400(String body) throws Exception {
		mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
		verify(service, never()).create(any());
	}

	@Test
	void tooLongUrlReturns400() throws Exception {
		String url = "https://example.com/" + "a".repeat(2040);
		mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"" + url + "\"}"))
				.andExpect(status().isBadRequest());
		verify(service, never()).create(any());
	}
}
