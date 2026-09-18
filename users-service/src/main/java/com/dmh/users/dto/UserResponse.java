package com.dmh.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

	private Long id;
	private String name;
	private String lastName;
	private String dni;
	private String email;
	private String phoneNumber;
	private String cvu;
	private String alias;
}
