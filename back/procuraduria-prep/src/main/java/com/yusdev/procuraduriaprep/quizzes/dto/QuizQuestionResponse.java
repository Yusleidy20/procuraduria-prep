package com.yusdev.procuraduriaprep.quizzes.dto;

import java.util.List;
import java.util.UUID;

import com.yusdev.procuraduriaprep.quizzes.entity.QuizQuestion;

public record QuizQuestionResponse(
		UUID id,
		UUID quizId,
		String statement,
		List<String> options,
		String explanationReference) {

	public static QuizQuestionResponse from(QuizQuestion question) {
		return new QuizQuestionResponse(question.getId(), question.getQuiz().getId(), question.getStatement(),
				question.getOptions(), question.getSourceDocument() == null
						? null
						: question.getSourceDocument().getOriginalName());
	}
}
