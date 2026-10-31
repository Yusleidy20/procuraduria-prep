package com.yusdev.procuraduriaprep.flashcards.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yusdev.procuraduriaprep.flashcards.entity.Flashcard;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

	Page<Flashcard> findAllByTopicId(UUID topicId, Pageable pageable);

	Page<Flashcard> findAllByNextReviewAtLessThanEqual(Instant now, Pageable pageable);

	long countByTopicId(UUID topicId);
}
