package com.yusdev.procuraduriaprep.quizzes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.yusdev.procuraduriaprep.quizzes.entity.QuizAttempt;

public record QuizAttemptResponse(
        UUID id,
        UUID quizId,
        Instant startedAt,
        Instant deadlineAt,
        Instant finishedAt,
        int totalQuestions,
        Integer correctAnswers,
        Integer incorrectAnswers,
        BigDecimal scorePercentage,
        List<QuizAnswerResultResponse> answers) {

    public static QuizAttemptResponse from(QuizAttempt attempt, List<QuizAnswerResultResponse> answers) {
        return new QuizAttemptResponse(attempt.getId(), attempt.getQuiz().getId(), attempt.getStartedAt(),
                attempt.getDeadlineAt(), attempt.getFinishedAt(), attempt.getTotalQuestions(), attempt.getCorrectAnswers(),
                attempt.getIncorrectAnswers(), attempt.getScorePercentage(), List.copyOf(answers));
    }
}
