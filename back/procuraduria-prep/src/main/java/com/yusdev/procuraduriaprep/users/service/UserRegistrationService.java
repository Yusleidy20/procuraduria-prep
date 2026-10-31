package com.yusdev.procuraduriaprep.users.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.shared.exception.DuplicateResourceException;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.users.dto.RegisterUserRequest;
import com.yusdev.procuraduriaprep.users.dto.UserResponse;
import com.yusdev.procuraduriaprep.users.entity.AppUser;
import com.yusdev.procuraduriaprep.users.repository.AppUserRepository;

@Service
@Transactional(readOnly = true)
public class UserRegistrationService {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserRegistrationService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse registerFirstUser(RegisterUserRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new InvalidRequestException("La contraseña no puede superar 72 bytes UTF-8");
		}
		if (userRepository.count() > 0) {
			throw new InvalidRequestException("El registro está cerrado: esta instalación ya tiene una cuenta");
		}
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateResourceException("Ya existe una cuenta con ese correo electrónico");
		}
		AppUser user = userRepository.save(
				new AppUser(request.name().trim(), email, passwordEncoder.encode(request.password())));
		return UserResponse.from(user);
	}
}
