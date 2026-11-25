package com.learninglab.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.learninglab.urlshortener.entity.ShortUrl;
import com.learninglab.urlshortener.repository.ShortUrlRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ShortUrlCreationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShortUrlRepository repository;

	@Test
	void postPersistsShortUrl() throws Exception {
		String body = mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/some/path?q=1\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String code = JsonPath.read(body, "$.code");
		ShortUrl saved = repository.findByCode(code).orElseThrow();
		assertThat(saved.getOriginalUrl()).isEqualTo("https://example.com/some/path?q=1");
	}

	@Test
	void postStripsSurroundingWhitespace() throws Exception {
		String body = mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"  https://example.com/spaced  \"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String code = JsonPath.read(body, "$.code");
		ShortUrl saved = repository.findByCode(code).orElseThrow();
		assertThat(saved.getOriginalUrl()).isEqualTo("https://example.com/spaced");
	}
}
