package com.yusdev.procuraduriaprep.flashcards.entity;

import java.time.Instant;
import java.util.UUID;

import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.Topic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "flashcards")
public class Flashcard {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, length = 2000)
	private String question;

	@Column(nullable = false, length = 5000)
	private String answer;

	@Column(length = 3000)
	private String explanation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_document_id")
	private StudyDocument sourceDocument;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DifficultyLevel difficulty;

	@Column(name = "review_count", nullable = false)
	private int reviewCount;

	@Column(name = "correct_review_count", nullable = false)
	private int correctReviewCount;

	@Column(name = "next_review_at", nullable = false)
	private Instant nextReviewAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Flashcard() {
	}

	public Flashcard(String question, String answer, String explanation, Topic topic,
			StudyDocument sourceDocument, DifficultyLevel difficulty) {
		this.question = question;
		this.answer = answer;
		this.explanation = explanation;
		this.topic = topic;
		this.sourceDocument = sourceDocument;
		this.difficulty = difficulty;
		this.reviewCount = 0;
		this.correctReviewCount = 0;
		this.createdAt = Instant.now();
		this.nextReviewAt = this.createdAt;
	}

	public void recordReview(boolean remembered, Instant reviewedAt) {
		this.reviewCount++;
		if (remembered) {
			this.correctReviewCount++;
		}
		this.nextReviewAt = remembered ? reviewedAt.plusSeconds(24 * 60 * 60) : reviewedAt.plusSeconds(10 * 60);
	}

	public UUID getId() {
		return id;
	}

	public String getQuestion() {
		return question;
	}

	public String getAnswer() {
		return answer;
	}

	public String getExplanation() {
		return explanation;
	}

	public Topic getTopic() {
		return topic;
	}

	public StudyDocument getSourceDocument() {
		return sourceDocument;
	}

	public DifficultyLevel getDifficulty() {
		return difficulty;
	}

	public int getReviewCount() {
		return reviewCount;
	}

	public int getCorrectReviewCount() {
		return correctReviewCount;
	}

	public Instant getNextReviewAt() {
		return nextReviewAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
