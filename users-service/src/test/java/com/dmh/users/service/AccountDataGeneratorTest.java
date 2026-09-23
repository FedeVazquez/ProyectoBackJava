package com.dmh.users.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class AccountDataGeneratorTest {
	private final AccountDataGenerator generator = new AccountDataGenerator();
	
	@Test
	void cvuHasTwentyTwoDigits() {
		assertTrue(generator.generateCvu().matches("\\d{22}"));
	}
	
	@RepeatedTest(50)
	void aliasHasThreeDifferentWordsSeparatedByDots() {
		String alias = generator.generateAlias();
		String[] parts = alias.split("\\.");

		assertTrue(alias.matches("[a-z]+\\.[a-z]+\\.[a-z]+"));
		assertEquals(3, parts.length);
		assertEquals(3, new HashSet<>(List.of(parts)).size());
	}
	
	@Test
	void twoGeneratedCvusAreDifferent() {
		assertNotEquals(generator.generateCvu(), generator.generateCvu());
	}
}
