package com.yusdev.procuraduriaprep.users.dto;

import java.time.Instant;
import java.util.UUID;

import com.yusdev.procuraduriaprep.users.entity.AppUser;

public record UserResponse(UUID id, String name, String email, Instant createdAt) {

	public static UserResponse from(AppUser user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
	}
}
