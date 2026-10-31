package com.yusdev.procuraduriaprep.ai.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GeneratedQuizQuestion(
		@NotBlank @Size(max = 3000) String statement,
		@NotEmpty @Size(min = 2, max = 6) List<@NotBlank @Size(max = 500) String> options,
		@NotNull @Min(0) Integer correctOptionIndex,
		@Size(max = 3000) String explanation,
		List<DocumentSourceReference> sources) {

	public GeneratedQuizQuestion(String statement, List<String> options, Integer correctOptionIndex, String explanation) {
		this(statement, options, correctOptionIndex, explanation, List.of());
	}

	public GeneratedQuizQuestion {
		sources = sources == null ? List.of() : List.copyOf(sources);
	}
}
