package com.learninglab.urlshortener.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.learninglab.urlshortener.TestcontainersConfiguration;
import com.learninglab.urlshortener.entity.ShortUrl;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ShortUrlRepositoryTest {

	@Autowired
	private ShortUrlRepository repository;

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
}
