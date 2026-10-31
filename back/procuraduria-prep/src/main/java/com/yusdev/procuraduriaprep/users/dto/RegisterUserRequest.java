package com.yusdev.procuraduriaprep.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 12, max = 72) String password) {
}
