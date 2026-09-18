package com.dmh.user.dto;

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
	private String name;
	
	@NotBlank(message = "El apellido es obligatorio")
	private String lastName;
	
	@NotBlank(message = "El DNI es obligatorio")
	@Pattern(regexp = "\\d{7,8}", message = "El dni debe tener entre 7 y 8 digitos")
	private String dni;
	
	@NotBlank(message = "El mail es obligatorio")
	@Email(message = "El mail no tiene un formato valido")
	private String email;
	
	@NotBlank(message = "El telefono es obligatorio")
	private String phoneNumber;
	
	@NotBlank(message = "La contraseña es obligatoria")
	@Size(min = 8, message = "La contrasenia debe tener al menos 8 caracteres")
	private String password;
	
}
