package com.yusdev.procuraduriaprep.flashcards.dto;

import java.time.Instant;
import java.util.UUID;

import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.flashcards.entity.Flashcard;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

public record FlashcardResponse(
		UUID id,
		String question,
		String answer,
		String explanation,
		UUID topicId,
		UUID sourceDocumentId,
		DifficultyLevel difficulty,
		int reviewCount,
		int correctReviewCount,
		Instant nextReviewAt,
		Instant createdAt) {

	public static FlashcardResponse from(Flashcard flashcard) {
		StudyDocument source = flashcard.getSourceDocument();
		return new FlashcardResponse(flashcard.getId(), flashcard.getQuestion(), flashcard.getAnswer(),
				flashcard.getExplanation(), flashcard.getTopic().getId(), source == null ? null : source.getId(),
				flashcard.getDifficulty(), flashcard.getReviewCount(), flashcard.getCorrectReviewCount(),
				flashcard.getNextReviewAt(), flashcard.getCreatedAt());
	}
}
