package com.yusdev.procuraduriaprep.flashcards.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yusdev.procuraduriaprep.flashcards.entity.FlashcardReview;

public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, UUID> {

	long countByRememberedTrue();

	long countByRememberedFalse();

	long countByFlashcardTopicIdAndRememberedTrue(UUID topicId);

	long countByFlashcardTopicIdAndRememberedFalse(UUID topicId);
}
