package com.yusdev.procuraduriaprep.quizzes.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.yusdev.procuraduriaprep.quizzes.entity.Quiz;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

public record QuizResponse(
        UUID id,
        String name,
        String description,
        UUID topicId,
        Set<UUID> topicIds,
        boolean mockExam,
        DifficultyLevel difficulty,
        Integer durationMinutes,
        long questionCount,
        Instant createdAt) {

    public static QuizResponse from(Quiz quiz, long questionCount) {
        return new QuizResponse(quiz.getId(), quiz.getName(), quiz.getDescription(), quiz.getTopic().getId(),
                quiz.getTopicIds(), quiz.isMockExam(), quiz.getDifficulty(), quiz.getDurationMinutes(),
                questionCount, quiz.getCreatedAt());
    }
}
