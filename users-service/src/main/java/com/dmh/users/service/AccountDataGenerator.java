package com.dmh.users.service;

import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;


@Component
public class AccountDataGenerator {

	private static final int CVU_LENGHT = 22;
	private static final int ALIAS_WORDS = 3;
	
	private final SecureRandom random = new SecureRandom();
	private final List<String> words;
	
	public AccountDataGenerator() {
		this.words = loadWords();
	}
	
	public String generateCvu() {
		StringBuilder cvu = new StringBuilder(CVU_LENGHT);
		for(int i = 0; i < CVU_LENGHT; i++) {
			cvu.append(random.nextInt(10));
		}
		return cvu.toString();
	}
	
	public String generateAlias() {
		List<String> shufled = new ArrayList<>(words);
		Collections.shuffle(shufled, random);
		return String.join(".", shufled.subList(0, ALIAS_WORDS));	
	}
	
	private List<String> loadWords(){
		try(BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ClassPathResource("aliases.txt").getInputStream(),
				StandardCharsets.UTF_8))) {
								List<String> loaded = reader.lines().map(String::trim).filter(line ->
								!line.isEmpty()).toList();
								
								if (loaded.size() < ALIAS_WORDS) {
									throw new IllegalStateException("aliases.txt necesita al menos " + ALIAS_WORDS + " "
											+ " palabras ");
								}
			return loaded;
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer aliases.txt", e);
		}
	}
	
}
