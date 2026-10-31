package com.yusdev.procuraduriaprep.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.yusdev.procuraduriaprep.ai.dto.GeneratedFlashcard;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedQuizQuestion;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.Validation;

class AiOutputValidatorTest {

	private final AiOutputValidator validator = new AiOutputValidator(
			Validation.buildDefaultValidatorFactory().getValidator());

	@Test
	void acceptsValidStructuredQuizOutput() {
		var output = validator.validateQuestions(List.of(
				new GeneratedQuizQuestion("Pregunta", List.of("A", "B"), 1, "Explicación")), 1);

		assertEquals(1, output.size());
	}

	@Test
	void rejectsCorrectOptionThatDoesNotExist() {
		assertThrows(AiOutputValidationException.class,
				() -> validator.validateQuestions(List.of(
						new GeneratedQuizQuestion("Pregunta", List.of("A", "B"), 2, null)), 1));
	}

	@Test
	void rejectsWrongGeneratedItemCountAndInvalidFlashcardContent() {
		assertThrows(AiOutputValidationException.class,
				() -> validator.validateFlashcards(List.of(), 1));
		assertThrows(AiOutputValidationException.class,
				() -> validator.validateFlashcards(List.of(
						new GeneratedFlashcard(" ", "Respuesta", null, DifficultyLevel.BASIC)), 1));
	}

	@Test
	void rejectsMissingExplanation() {
		assertThrows(AiOutputValidationException.class,
				() -> validator.validate((TopicExplanationResponse) null));
	}
}
