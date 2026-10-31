package com.yusdev.procuraduriaprep.flashcards.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "flashcard_reviews")
public class FlashcardReview {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "flashcard_id", nullable = false)
	private Flashcard flashcard;

	@Column(nullable = false)
	private boolean remembered;

	@Column(name = "reviewed_at", nullable = false, updatable = false)
	private Instant reviewedAt;

	protected FlashcardReview() {
	}

	public FlashcardReview(Flashcard flashcard, boolean remembered, Instant reviewedAt) {
		this.flashcard = flashcard;
		this.remembered = remembered;
		this.reviewedAt = reviewedAt;
	}

	public UUID getId() {
		return id;
	}

	public Flashcard getFlashcard() {
		return flashcard;
	}

	public boolean isRemembered() {
		return remembered;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}
}
