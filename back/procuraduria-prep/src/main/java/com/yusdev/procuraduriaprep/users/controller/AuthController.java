package com.yusdev.procuraduriaprep.users.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.yusdev.procuraduriaprep.users.dto.RegisterUserRequest;
import com.yusdev.procuraduriaprep.users.dto.CsrfTokenResponse;
import com.yusdev.procuraduriaprep.users.dto.UserResponse;
import com.yusdev.procuraduriaprep.users.repository.AppUserRepository;
import com.yusdev.procuraduriaprep.users.service.UserRegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final UserRegistrationService registrationService;
	private final AppUserRepository userRepository;

	public AuthController(UserRegistrationService registrationService, AppUserRepository userRepository) {
		this.registrationService = registrationService;
		this.userRepository = userRepository;
	}

	@GetMapping("/csrf")
	public CsrfTokenResponse csrf(CsrfToken csrfToken) {
		return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(
			@Valid @RequestBody RegisterUserRequest request,
			UriComponentsBuilder uriBuilder) {
		UserResponse response = registrationService.registerFirstUser(request);
		URI location = uriBuilder.path("/api/v1/auth/me").build().toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping("/me")
	public UserResponse me(Authentication authentication) {
		var user = userRepository.findByEmailIgnoreCase(authentication.getName())
				.orElseThrow(() -> new IllegalStateException("La cuenta autenticada ya no existe"));
		return UserResponse.from(user);
	}
}
