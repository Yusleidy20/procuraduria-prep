package com.yusdev.procuraduriaprep.ai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.yusdev.procuraduriaprep.ai.dto.GeneratedFlashcard;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedQuizQuestion;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;

import jakarta.validation.Validator;

@Component
public class AiOutputValidator {

	private final Validator validator;

	public AiOutputValidator(Validator validator) {
		this.validator = validator;
	}

	public TopicExplanationResponse validate(TopicExplanationResponse response) {
		if (response == null) {
			throw new AiOutputValidationException(List.of("La explicación generada no puede ser nula"));
		}
		validateBean(response);
		return response;
	}

	public List<GeneratedFlashcard> validateFlashcards(List<GeneratedFlashcard> flashcards, int expectedCount) {
		validateCollectionSize(flashcards, expectedCount, "tarjetas");
		List<String> violations = new ArrayList<>();
		for (int index = 0; index < flashcards.size(); index++) {
			int itemIndex = index;
			GeneratedFlashcard flashcard = flashcards.get(index);
			if (flashcard == null) {
				violations.add("flashcards[%d]: el elemento no puede ser nulo".formatted(index));
				continue;
			}
			validator.validate(flashcard).forEach(violation ->
					violations.add("flashcards[%d].%s: %s".formatted(itemIndex, violation.getPropertyPath(),
							violation.getMessage())));
		}
		throwIfInvalid(violations);
		return List.copyOf(flashcards);
	}

	public List<GeneratedQuizQuestion> validateQuestions(List<GeneratedQuizQuestion> questions, int expectedCount) {
		validateCollectionSize(questions, expectedCount, "preguntas");
		List<String> violations = new ArrayList<>();
		for (int index = 0; index < questions.size(); index++) {
			int itemIndex = index;
			GeneratedQuizQuestion question = questions.get(index);
			if (question == null) {
				violations.add("questions[%d]: el elemento no puede ser nulo".formatted(index));
				continue;
			}
			validator.validate(question).forEach(violation ->
					violations.add("questions[%d].%s: %s".formatted(itemIndex, violation.getPropertyPath(),
							violation.getMessage())));
			if (question.correctOptionIndex() != null && question.options() != null
					&& question.correctOptionIndex() >= question.options().size()) {
				violations.add("questions[%d].correctOptionIndex: debe apuntar a una opción existente".formatted(index));
			}
		}
		throwIfInvalid(violations);
		return List.copyOf(questions);
	}

	private void validateBean(Object value) {
		List<String> violations = validator.validate(value).stream()
				.map(violation -> "%s: %s".formatted(violation.getPropertyPath(), violation.getMessage()))
				.toList();
		throwIfInvalid(violations);
	}

	private void validateCollectionSize(List<?> values, int expectedCount, String label) {
		if (expectedCount < 1 || expectedCount > 20) {
			throw new IllegalArgumentException("La cantidad esperada debe estar entre 1 y 20");
		}
		if (values == null || values.size() != expectedCount) {
			throw new AiOutputValidationException(
					List.of("La cantidad de %s generadas no coincide con la solicitada".formatted(label)));
		}
	}

	private void throwIfInvalid(List<String> violations) {
		if (!violations.isEmpty()) {
			throw new AiOutputValidationException(violations);
		}
	}
}
