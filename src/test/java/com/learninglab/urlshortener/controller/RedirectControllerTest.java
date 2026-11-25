package com.learninglab.urlshortener.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.learninglab.urlshortener.exception.ShortUrlNotFoundException;
import com.learninglab.urlshortener.service.ShortUrlService;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortUrlService service;

	@Test
	void knownCodeRedirectsWith302() throws Exception {
		when(service.getOriginalUrl("abc1234")).thenReturn("https://example.com/a?b=1");

		mockMvc.perform(get("/abc1234"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/a?b=1"))
				.andExpect(content().string(""));
	}

	@Test
	void unknownCodeReturns404() throws Exception {
		when(service.getOriginalUrl("nope123")).thenThrow(new ShortUrlNotFoundException("nope123"));

		mockMvc.perform(get("/nope123")).andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = { "/abc", "/abcdefgh", "/abc-def" })
	void codesNotMatchingPatternReturn404WithoutCallingService(String path) throws Exception {
		mockMvc.perform(get(path)).andExpect(status().isNotFound());

		verify(service, never()).getOriginalUrl(ArgumentMatchers.any());
	}
}
