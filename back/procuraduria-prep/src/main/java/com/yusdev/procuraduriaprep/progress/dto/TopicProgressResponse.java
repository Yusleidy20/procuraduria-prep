package com.yusdev.procuraduriaprep.progress.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopicProgressResponse(
		UUID topicId,
		String topicName,
		long completedQuizAttempts,
		long quizAnswers,
		long correctQuizAnswers,
		long flashcardCount,
		long flashcardReviews,
		long rememberedReviews,
		BigDecimal masteryPercentage) {
}
