package com.learninglab.urlshortener;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RedirectIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createdUrlRedirectsToOriginal() throws Exception {
		String body = mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/target?x=1\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String code = JsonPath.read(body, "$.code");

		mockMvc.perform(get("/" + code))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/target?x=1"));
	}

	@Test
	void unknownValidLookingCodeReturns404() throws Exception {
		mockMvc.perform(get("/Zz9Zz9Z")).andExpect(status().isNotFound());
	}

	@Test
	void healthRouteStillWins() throws Exception {
		mockMvc.perform(get("/api/health")).andExpect(status().isOk());
	}
}
