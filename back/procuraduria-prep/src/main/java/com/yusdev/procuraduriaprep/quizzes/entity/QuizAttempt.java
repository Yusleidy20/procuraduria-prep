package com.yusdev.procuraduriaprep.quizzes.entity;

import java.math.BigDecimal;
import java.time.Duration;
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
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    @Column(name = "incorrect_answers")
    private Integer incorrectAnswers;

    @Column(name = "score_percentage", precision = 5, scale = 2)
    private BigDecimal scorePercentage;

    protected QuizAttempt() {
    }

    public QuizAttempt(Quiz quiz, int totalQuestions) {
        this.quiz = quiz;
        this.totalQuestions = totalQuestions;
        this.startedAt = Instant.now();
    }

    public void finish(Instant finishedAt, int correctAnswers, int incorrectAnswers, BigDecimal scorePercentage) {
        this.finishedAt = finishedAt;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.scorePercentage = scorePercentage;
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public Instant getDeadlineAt() {
        Integer durationMinutes = quiz.getDurationMinutes();
        return durationMinutes == null ? null : startedAt.plus(Duration.ofMinutes(durationMinutes));
    }

    public boolean isExpired(Instant now) {
        Instant deadlineAt = getDeadlineAt();
        return deadlineAt != null && !now.isBefore(deadlineAt);
    }

    public UUID getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public Integer getCorrectAnswers() {
        return correctAnswers;
    }

    public Integer getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public BigDecimal getScorePercentage() {
        return scorePercentage;
    }
}
