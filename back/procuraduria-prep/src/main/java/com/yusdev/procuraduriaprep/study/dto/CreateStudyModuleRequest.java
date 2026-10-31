package com.yusdev.procuraduriaprep.study.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudyModuleRequest(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 1000) String description,
		@Min(0) int displayOrder) {
}
