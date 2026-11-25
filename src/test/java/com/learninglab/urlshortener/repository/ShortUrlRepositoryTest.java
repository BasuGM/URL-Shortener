package com.learninglab.urlshortener.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.learninglab.urlshortener.TestcontainersConfiguration;
import com.learninglab.urlshortener.entity.ShortUrl;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ShortUrlRepositoryTest {

	@Autowired
	private ShortUrlRepository repository;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void saveThenFindByCodePopulatesIdAndCreatedAt() {
		repository.saveAndFlush(new ShortUrl("abc123", "https://example.com"));

		ShortUrl found = repository.findByCode("abc123").orElseThrow();

		assertThat(found.getId()).isNotNull();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getOriginalUrl()).isEqualTo("https://example.com");
	}

	@Test
	void findByCodeUnknownReturnsEmpty() {
		assertThat(repository.findByCode("missing")).isEmpty();
	}

	@Test
	void existsByCode() {
		repository.saveAndFlush(new ShortUrl("abc123", "https://example.com"));

		assertThat(repository.existsByCode("abc123")).isTrue();
		assertThat(repository.existsByCode("nope")).isFalse();
	}

	@Test
	void duplicateCodeViolatesUniqueConstraint() {
		repository.saveAndFlush(new ShortUrl("dup", "https://example.com/a"));

		assertThatThrownBy(() -> repository.saveAndFlush(new ShortUrl("dup", "https://example.com/b")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void incrementClickCountUpdatesCountAndTimestamp() {
		repository.saveAndFlush(new ShortUrl("clk1234", "https://example.com"));
		Instant now = Instant.parse("2026-01-02T03:04:05Z");

		assertThat(repository.incrementClickCount("clk1234", now)).isEqualTo(1);
		assertThat(repository.incrementClickCount("clk1234", now)).isEqualTo(1);

		Long count = jdbc.queryForObject("select click_count from short_urls where code = 'clk1234'", Long.class);
		assertThat(count).isEqualTo(2);
		ShortUrl found = repository.findByCode("clk1234").orElseThrow();
		assertThat(found.getClickCount()).isEqualTo(2);
		assertThat(found.getLastAccessedAt()).isEqualTo(now);
	}

	@Test
	void incrementClickCountUnknownCodeReturnsZero() {
		assertThat(repository.incrementClickCount("missing", Instant.now())).isZero();
	}
}
