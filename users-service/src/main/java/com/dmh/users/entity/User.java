package com.dmh.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, unique = true, length = 20)
	private String dni;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(nullable = false, length = 20)
	private String phoneNumber;

	@Column(nullable = false)
	private String password;
	
	@Column(nullable = false, unique = true, length = 22)
	private String cvu;
	
	@Column(nullable = false, unique = true, length = 100)
	private String alias;

}
