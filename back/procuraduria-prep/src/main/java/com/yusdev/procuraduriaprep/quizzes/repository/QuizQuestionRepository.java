package com.yusdev.procuraduriaprep.quizzes.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yusdev.procuraduriaprep.quizzes.entity.QuizQuestion;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    Page<QuizQuestion> findAllByQuizId(UUID quizId, Pageable pageable);

    long countByQuizId(UUID quizId);

    long countByQuizIdAndId(UUID quizId, UUID questionId);

    @Query("""
            select question from QuizQuestion question
            left join question.topic sourceTopic
            join question.quiz quiz
            where coalesce(sourceTopic.id, quiz.topic.id) in :topicIds
              and quiz.difficulty = :difficulty
            """)
    List<QuizQuestion> findExamCandidates(@Param("topicIds") List<UUID> topicIds,
                                          @Param("difficulty") DifficultyLevel difficulty, Pageable pageable);
}
