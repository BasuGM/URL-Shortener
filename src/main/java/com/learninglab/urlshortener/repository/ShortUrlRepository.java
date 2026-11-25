package com.learninglab.urlshortener.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.learninglab.urlshortener.entity.ShortUrl;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

	Optional<ShortUrl> findByCode(String code);

	boolean existsByCode(String code);

	// Single atomic UPDATE: the database does the increment, so concurrent clicks never lose updates.
	@Modifying(clearAutomatically = true)
	@Query("update ShortUrl s set s.clickCount = s.clickCount + 1, s.lastAccessedAt = :now where s.code = :code")
	int incrementClickCount(@Param("code") String code, @Param("now") Instant now);
}
