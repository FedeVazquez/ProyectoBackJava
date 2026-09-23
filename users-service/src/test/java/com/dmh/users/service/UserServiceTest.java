package com.dmh.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dmh.users.dto.RegisterUserRequest;
import com.dmh.users.dto.UserResponse;
import com.dmh.users.entity.User;
import com.dmh.users.exception.UserAlreadyExistsException;
import com.dmh.users.repository.UserRepository;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {
	
	@Mock
	private UserRepository userRepository;
	
	@Mock
	private PasswordEncoder passwordEncoder;
	
	@Mock
	private AccountDataGenerator accountDataGenerator;
	
	@InjectMocks
	private UserService userService;
	
	@Test
	void registerSaveUserWithNormalizedEmailHashedPasswordAndGenerateData() {
		when(passwordEncoder.encode("clave1234")).thenReturn("hash");
		when(accountDataGenerator.generateCvu()).thenReturn("1234567890123456789012");
		when(accountDataGenerator.generateAlias()).thenReturn("sol.luna.mar");
		when(userRepository.save(any(User.class))).thenAnswer(invocation ->
		invocation.getArgument(0));
		
		
		UserResponse response = userService.register(validRequest());
		
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();
		
		assertEquals("fede@mail.com", saved.getEmail());
		assertEquals("hash", saved.getPassword());
		assertNotEquals("clave1234", saved.getPassword());
		assertEquals("1234567890123456789012", saved.getCvu());
		assertEquals("sol.luna.mar", saved.getAlias());
		
	}
	
	@Test
	void registerFailsWhenEmailAlreadyExists() {
		when(userRepository.existsByEmail("fede@mail.com")).thenReturn(true);
		
		assertThrows(UserAlreadyExistsException.class, () -> userService.register(validRequest()));
	
		verify(userRepository, never()).save(any(User.class));
	}
	
	@Test
	void registerFailsWhenDniAlreadyExists() {
		when(userRepository.existsByDni("12345678")).thenReturn(true);
		
		assertThrows(UserAlreadyExistsException.class, () -> userService.register(validRequest()));
		
		verify(userRepository, never()).save(any(User.class));
	}
	
	private RegisterUserRequest validRequest() {
		RegisterUserRequest request = new RegisterUserRequest();
		request.setName("Federico");
		request.setLastName("Vazquez");
		request.setDni("12345678");
		request.setEmail(" Fede@Mail.com ");
		request.setPhoneNumber("1144445555");
		request.setPassword("clave1234");
		return request;
	}

}
