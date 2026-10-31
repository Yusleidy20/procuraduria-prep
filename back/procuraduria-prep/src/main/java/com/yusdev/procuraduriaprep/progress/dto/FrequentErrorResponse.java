package com.yusdev.procuraduriaprep.progress.dto;

import java.util.UUID;

import com.yusdev.procuraduriaprep.quizzes.repository.FrequentQuizError;

public record FrequentErrorResponse(
		UUID questionId,
		String questionStatement,
		UUID topicId,
		String topicName,
		long errorCount) {

	public static FrequentErrorResponse from(FrequentQuizError error) {
		return new FrequentErrorResponse(error.getQuestionId(), error.getQuestionStatement(),
				error.getTopicId(), error.getTopicName(), error.getErrorCount());
	}
}
