package com.yusdev.procuraduriaprep.quizzes.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yusdev.procuraduriaprep.quizzes.entity.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    long countByFinishedAtIsNotNull();

    @Query("""
            select count(distinct attempt.id)
            from QuizAttempt attempt
            join QuizQuestion question on question.quiz = attempt.quiz
            left join question.topic sourceTopic
            where coalesce(sourceTopic.id, attempt.quiz.topic.id) = :topicId
              and attempt.finishedAt is not null
            """)
    long countFinishedByQuestionTopicId(@Param("topicId") UUID topicId);
}
