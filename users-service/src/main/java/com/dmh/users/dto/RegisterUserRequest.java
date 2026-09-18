package com.dmh.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserRequest {

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
	private String name;

	@NotBlank(message = "El apellido es obligatorio")
	@Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
	private String lastName;

	@NotBlank(message = "El DNI es obligatorio")
	@Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener entre 7 y 8 dígitos")
	private String dni;

	@NotBlank(message = "El email es obligatorio")
	@Email(message = "El email no tiene un formato válido")
	@Size(max = 254, message = "El email no puede superar los 254 caracteres")
	private String email;

	@NotBlank(message = "El teléfono es obligatorio")
	@Pattern(regexp = "\\d{8,15}", message = "El teléfono debe tener entre 8 y 15 dígitos")
	private String phoneNumber;

	@NotBlank(message = "La contraseña es obligatoria")
	@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
	private String password;

}
