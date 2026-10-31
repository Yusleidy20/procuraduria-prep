package com.yusdev.procuraduriaprep.quizzes.dto;

import java.util.UUID;

import com.yusdev.procuraduriaprep.quizzes.entity.QuizAnswer;

public record QuizAnswerResultResponse(
		UUID questionId,
		int selectedOptionIndex,
		int correctOptionIndex,
		boolean correct,
		String explanation) {

	public static QuizAnswerResultResponse from(QuizAnswer answer) {
		return new QuizAnswerResultResponse(answer.getQuestion().getId(), answer.getSelectedOptionIndex(),
				answer.getQuestion().getCorrectOptionIndex(), answer.isCorrect(), answer.getQuestion().getExplanation());
	}
}
