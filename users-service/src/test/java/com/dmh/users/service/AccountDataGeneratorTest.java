package com.dmh.users.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AccountDataGeneratorTest {
	private final AccountDataGenerator generator = new AccountDataGenerator();
	
	@Test
	void cvuHasTwentyTwoDigits() {
		assertTrue(generator.generateCvu().matches("\\d{22}"));
	}
	
	@Test
	void aliasHasThreeDifferentWordsSeparatedByDots() {
		String alias = generator.generateAlias();
		
		assertTrue(alias.matches("[a-z]+\\.[a-z]+\\.[a-z]+"));
		assertEquals(3, alias.split("\\.").length);
	}
	
	@Test
	void twoGeneratedCvusAreDifferent() {
		assertNotEquals(generator.generateCvu(), generator.generateCvu());
	}
}
