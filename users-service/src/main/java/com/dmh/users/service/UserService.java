package com.dmh.users.service;

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

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

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

		User saved = userRepository.save(user);

		return toResponse(saved);

	}

	private UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getLastName(), user.getDni(), user.getEmail(),
				user.getPhoneNumber(), null, null);
	}

}
