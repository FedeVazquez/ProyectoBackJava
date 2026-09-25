package com.dmh.users.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dmh.users.dto.LoginRequest;
import com.dmh.users.dto.LoginResponse;
import com.dmh.users.entity.User;
import com.dmh.users.exception.InvalidCredentialsException;
import com.dmh.users.exception.UserNotFoundException;
import com.dmh.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		String email = request.getEmail().trim().toLowerCase();

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("No existe un usuario con ese email"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new InvalidCredentialsException("La contraseña es incorrecta");
		}

		return new LoginResponse(jwtService.generateToken(user.getEmail()));
	}
}
