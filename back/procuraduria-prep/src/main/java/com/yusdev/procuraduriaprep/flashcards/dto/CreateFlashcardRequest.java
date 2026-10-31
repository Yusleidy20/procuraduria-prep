package com.yusdev.procuraduriaprep.flashcards.dto;

import java.util.UUID;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFlashcardRequest(
		@NotBlank @Size(max = 2000) String question,
		@NotBlank @Size(max = 5000) String answer,
		@Size(max = 3000) String explanation,
		@NotNull DifficultyLevel difficulty,
		UUID sourceDocumentId) {
}
