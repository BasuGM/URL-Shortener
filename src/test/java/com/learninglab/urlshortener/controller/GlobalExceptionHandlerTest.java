package com.learninglab.urlshortener.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.learninglab.urlshortener.dto.CreateShortUrlRequest;
import com.learninglab.urlshortener.exception.ShortUrlNotFoundException;
import com.learninglab.urlshortener.service.ShortUrlService;

@WebMvcTest({ ShortUrlController.class, RedirectController.class, HealthController.class })
class GlobalExceptionHandlerTest {

	private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortUrlService service;

	@Test
	void invalidSchemeReturnsProblemWithFieldErrors() throws Exception {
		mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"ftp://x.com\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Validation failed"))
				.andExpect(jsonPath("$.instance").value("/api/urls"))
				.andExpect(jsonPath("$.errors[0].field").value("url"))
				.andExpect(jsonPath("$.errors[0].message").isNotEmpty());
	}

	@Test
	void missingUrlReturnsErrorsForUrl() throws Exception {
		mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM))
				.andExpect(jsonPath("$.errors[0].field").value("url"));
	}

	@Test
	void malformedJsonReturnsGenericDetail() throws Exception {
		mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content("{\"url\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM))
				.andExpect(jsonPath("$.detail").value("Malformed JSON request body"))
				.andExpect(jsonPath("$.instance").value("/api/urls"));
	}

	@Test
	void unknownCodeReturns404Problem() throws Exception {
		when(service.resolveRedirect("nope123")).thenThrow(new ShortUrlNotFoundException("nope123"));

		mockMvc.perform(get("/nope123"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM))
				.andExpect(jsonPath("$.title").value("Not Found"))
				.andExpect(jsonPath("$.detail").value(containsString("nope123")))
				.andExpect(jsonPath("$.instance").value("/nope123"));
	}

	@Test
	void nonMatchingPathReturns404Problem() throws Exception {
		mockMvc.perform(get("/abc"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM))
				.andExpect(jsonPath("$.instance").value("/abc"));
	}

	@Test
	void wrongMethodReturns405Problem() throws Exception {
		mockMvc.perform(post("/api/health"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM));
	}

	@Test
	void unexpectedExceptionReturns500WithoutLeakingMessage() throws Exception {
		when(service.create(any(CreateShortUrlRequest.class))).thenThrow(new RuntimeException("db password=secret"));

		mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM))
				.andExpect(jsonPath("$.title").value("Internal Server Error"))
				.andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
				.andExpect(content().string(not(containsString("secret"))));
	}
}
