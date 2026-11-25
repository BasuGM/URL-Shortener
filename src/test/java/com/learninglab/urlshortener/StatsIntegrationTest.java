package com.learninglab.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.learninglab.urlshortener.service.ShortUrlService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StatsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShortUrlService shortUrlService;

	private String createCode(String url) throws Exception {
		String body = mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"" + url + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.code");
	}

	@Test
	void redirectsAreCountedAndExposedInStats() throws Exception {
		String code = createCode("https://example.com/counted");

		mockMvc.perform(get("/" + code)).andExpect(status().isFound());
		mockMvc.perform(get("/" + code)).andExpect(status().isFound());

		mockMvc.perform(get("/api/urls/" + code + "/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clickCount").value(2))
				.andExpect(jsonPath("$.lastAccessedAt").isNotEmpty());
	}

	@Test
	void newUrlHasZeroClicksAndNoLastAccess() throws Exception {
		String code = createCode("https://example.com/fresh");

		mockMvc.perform(get("/api/urls/" + code + "/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clickCount").value(0))
				.andExpect(jsonPath("$.lastAccessedAt").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void concurrentRedirectsAreAllCounted() throws Exception {
		String code = createCode("https://example.com/concurrent");
		int threads = 20;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		try {
			for (int i = 0; i < threads; i++) {
				futures.add(pool.submit(() -> {
					ready.countDown();
					start.await();
					return shortUrlService.resolveRedirect(code);
				}));
			}
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			for (Future<?> f : futures) {
				f.get(30, TimeUnit.SECONDS);
			}
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(shortUrlService.getStats(code).clickCount()).isEqualTo(threads);
	}
}
