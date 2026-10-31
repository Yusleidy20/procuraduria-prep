package com.yusdev.procuraduriaprep.quizzes.entity;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "quiz_answers", uniqueConstraints = @UniqueConstraint(
		name = "uk_attempt_question", columnNames = { "attempt_id", "question_id" }))
public class QuizAnswer {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_id", nullable = false)
	private QuizAttempt attempt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private QuizQuestion question;

	@Column(name = "selected_option_index", nullable = false)
	private int selectedOptionIndex;

	@Column(nullable = false)
	private boolean correct;

	@Column(name = "answered_at", nullable = false, updatable = false)
	private Instant answeredAt;

	protected QuizAnswer() {
	}

	public QuizAnswer(QuizAttempt attempt, QuizQuestion question, int selectedOptionIndex, Instant answeredAt) {
		this.attempt = attempt;
		this.question = question;
		this.selectedOptionIndex = selectedOptionIndex;
		this.correct = selectedOptionIndex == question.getCorrectOptionIndex();
		this.answeredAt = answeredAt;
	}

	public UUID getId() {
		return id;
	}

	public QuizAttempt getAttempt() {
		return attempt;
	}

	public QuizQuestion getQuestion() {
		return question;
	}

	public int getSelectedOptionIndex() {
		return selectedOptionIndex;
	}

	public boolean isCorrect() {
		return correct;
	}

	public Instant getAnsweredAt() {
		return answeredAt;
	}
}
