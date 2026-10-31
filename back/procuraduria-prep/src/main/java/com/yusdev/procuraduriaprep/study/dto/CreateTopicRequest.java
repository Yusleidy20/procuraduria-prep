package com.yusdev.procuraduriaprep.study.dto;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
		@NotBlank @Size(max = 160) String name,
		@Size(max = 2000) String description,
		@NotNull DifficultyLevel difficulty) {
}
