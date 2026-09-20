package com.dmh.users.service;

import java.util.Iterator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dmh.users.dto.RegisterUserRequest;
import com.dmh.users.dto.UserResponse;
import com.dmh.users.entity.User;
import com.dmh.users.exception.UserAlreadyExistsException;
import com.dmh.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private static final int MAX_ATTEMPTS = 10;
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccountDataGenerator accountDataGenerator;

	@Transactional
	public UserResponse register(RegisterUserRequest request) {
		String email = request.getEmail().trim().toLowerCase();

		if (userRepository.existsByEmail(email)) {
			throw new UserAlreadyExistsException("Ya existe un usuario registrado con esa dirección de e-mail");
		}

		if (userRepository.existsByDni(request.getDni())) {
			throw new UserAlreadyExistsException("Ya existe un usuario registrado con ese DNI");
		}

		User user = new User();
		user.setName(request.getName().trim());
		user.setLastName(request.getLastName().trim());
		user.setDni(request.getDni());
		user.setEmail(email);
		user.setPhoneNumber(request.getPhoneNumber());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setCvu(generateUniqueCvu());
		user.setAlias(generateUniqueAlias());
		User saved = userRepository.save(user);

		return toResponse(saved);

	}
	
	private String generateUniqueCvu() {
		for(int i = 0; i < MAX_ATTEMPTS; i++) {
			String cvu = accountDataGenerator.generateCvu();
			if (!userRepository.existsByCvu(cvu)) {
				return cvu;
			}
		}
		throw new IllegalStateException("Nose pudo generar un CVU unico");
	}
	
	
	private String generateUniqueAlias() {
		for(int i = 0; i < MAX_ATTEMPTS; i++) {
			String alias = accountDataGenerator.generateAlias();
			if (!userRepository.existsByAlias(alias)) {
				return alias;
			}
		}
		throw new IllegalStateException("Nose pudo generar alias unico");
	}

	private UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getLastName(), user.getDni(), user.getEmail(),
				user.getPhoneNumber(), user.getCvu(), user.getAlias());
	}

}
