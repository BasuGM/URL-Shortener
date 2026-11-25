package com.learninglab.urlshortener;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocsListTheEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/api/urls")))
				.andExpect(content().string(containsString("/api/urls/{code}/stats")))
				.andExpect(jsonPath("$.info.title").value("URL Shortener API"));
	}

	@Test
	void swaggerUiIsNotSwallowedByTheRedirectRoute() throws Exception {
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
	}
}
