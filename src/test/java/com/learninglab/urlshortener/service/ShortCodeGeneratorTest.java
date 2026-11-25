package com.learninglab.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

	private final ShortCodeGenerator generator = new ShortCodeGenerator();

	@Test
	void generatesSevenBase62Characters() {
		for (int i = 0; i < 200; i++) {
			assertThat(generator.generate()).hasSize(7).matches("[0-9A-Za-z]{7}");
		}
	}

	@Test
	void batchIsNotAllEqual() {
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 20; i++) {
			codes.add(generator.generate());
		}
		assertThat(codes.size()).isGreaterThan(1);
	}
}
