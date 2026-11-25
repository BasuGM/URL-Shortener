package com.learninglab.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.learninglab.urlshortener.dto.CreateShortUrlRequest;
import com.learninglab.urlshortener.dto.ShortUrlResponse;
import com.learninglab.urlshortener.dto.ShortUrlStatsResponse;
import com.learninglab.urlshortener.entity.ShortUrl;
import com.learninglab.urlshortener.exception.ShortUrlNotFoundException;
import com.learninglab.urlshortener.repository.ShortUrlRepository;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

	@Mock
	private ShortUrlRepository repository;

	@Mock
	private ShortCodeGenerator generator;

	private ShortUrlService service;

	@BeforeEach
	void setUp() {
		service = new ShortUrlService(repository, generator, "http://short.test/");
	}

	@Test
	void savesWithGeneratedCodeAndReturnsResponse() {
		when(generator.generate()).thenReturn("abc1234");
		when(repository.existsByCode("abc1234")).thenReturn(false);
		when(repository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

		ShortUrlResponse response = service.create(new CreateShortUrlRequest("https://example.com/x"));

		ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getCode()).isEqualTo("abc1234");
		assertThat(captor.getValue().getOriginalUrl()).isEqualTo("https://example.com/x");
		assertThat(response.code()).isEqualTo("abc1234");
		assertThat(response.shortUrl()).isEqualTo("http://short.test/abc1234");
		assertThat(response.originalUrl()).isEqualTo("https://example.com/x");
		assertThat(response.createdAt()).isNotNull();
	}

	@Test
	void retriesWhenFirstCodeExists() {
		when(generator.generate()).thenReturn("taken01", "free002");
		when(repository.existsByCode("taken01")).thenReturn(true);
		when(repository.existsByCode("free002")).thenReturn(false);
		when(repository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

		ShortUrlResponse response = service.create(new CreateShortUrlRequest("https://example.com"));

		assertThat(response.code()).isEqualTo("free002");
	}

	@Test
	void throwsAfterFiveCollisionsAndNeverSaves() {
		when(generator.generate()).thenReturn("taken01");
		when(repository.existsByCode("taken01")).thenReturn(true);

		assertThatThrownBy(() -> service.create(new CreateShortUrlRequest("https://example.com")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Could not generate a unique short code");

		verify(generator, org.mockito.Mockito.times(5)).generate();
		verify(repository, never()).save(any());
	}

	@Test
	void resolveRedirectIncrementsAndReturnsUrl() {
		when(repository.findByCode("abc1234")).thenReturn(Optional.of(new ShortUrl("abc1234", "https://example.com/x")));

		assertThat(service.resolveRedirect("abc1234")).isEqualTo("https://example.com/x");

		verify(repository).incrementClickCount(eq("abc1234"), any(Instant.class));
	}

	@Test
	void resolveRedirectThrowsForUnknownCodeAndNeverIncrements() {
		when(repository.findByCode("nope123")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resolveRedirect("nope123"))
				.isInstanceOf(ShortUrlNotFoundException.class)
				.hasMessage("Short URL not found: nope123");

		verify(repository, never()).incrementClickCount(any(), any());
	}

	@Test
	void getStatsMapsAllFields() {
		ShortUrl entity = new ShortUrl("abc1234", "https://example.com/x");
		ReflectionTestUtils.setField(entity, "clickCount", 7L);
		Instant last = Instant.parse("2026-01-02T00:00:00Z");
		ReflectionTestUtils.setField(entity, "lastAccessedAt", last);
		when(repository.findByCode("abc1234")).thenReturn(Optional.of(entity));

		ShortUrlStatsResponse stats = service.getStats("abc1234");

		assertThat(stats.code()).isEqualTo("abc1234");
		assertThat(stats.shortUrl()).isEqualTo("http://short.test/abc1234");
		assertThat(stats.originalUrl()).isEqualTo("https://example.com/x");
		assertThat(stats.clickCount()).isEqualTo(7);
		assertThat(stats.createdAt()).isEqualTo(entity.getCreatedAt());
		assertThat(stats.lastAccessedAt()).isEqualTo(last);
	}

	@Test
	void getStatsThrowsForUnknownCode() {
		when(repository.findByCode("nope123")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getStats("nope123")).isInstanceOf(ShortUrlNotFoundException.class);
	}
}
