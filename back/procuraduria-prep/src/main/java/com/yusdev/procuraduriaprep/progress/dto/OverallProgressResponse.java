package com.yusdev.procuraduriaprep.progress.dto;

import java.math.BigDecimal;

public record OverallProgressResponse(
		long topicCount,
		long completedQuizAttempts,
		long quizAnswers,
		long correctQuizAnswers,
		long flashcardCount,
		long flashcardReviews,
		long rememberedReviews,
		BigDecimal masteryPercentage) {
}
