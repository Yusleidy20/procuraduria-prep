package com.yusdev.procuraduriaprep.quizzes.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yusdev.procuraduriaprep.quizzes.entity.QuizAnswer;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {

    boolean existsByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    long countByAttemptIdAndCorrectTrue(UUID attemptId);

    List<QuizAnswer> findAllByAttemptIdOrderByAnsweredAtAsc(UUID attemptId);

    long countByCorrectTrue();

    long countByCorrectFalse();

    @Query("""
            select count(answer.id)
            from QuizAnswer answer
            join answer.question question
            join question.quiz quiz
            left join question.topic sourceTopic
            where coalesce(sourceTopic.id, quiz.topic.id) = :topicId and answer.correct = true
            """)
    long countCorrectByQuestionTopicId(@Param("topicId") UUID topicId);

    @Query("""
            select count(answer.id)
            from QuizAnswer answer
            join answer.question question
            join question.quiz quiz
            left join question.topic sourceTopic
            where coalesce(sourceTopic.id, quiz.topic.id) = :topicId and answer.correct = false
            """)
    long countIncorrectByQuestionTopicId(@Param("topicId") UUID topicId);

    @Query("""
            select q.id as questionId,
                   q.statement as questionStatement,
                   coalesce(t.id, quiz.topic.id) as topicId,
                   coalesce(t.name, quiz.topic.name) as topicName,
                   count(a.id) as errorCount
            from QuizAnswer a
            join a.question q
            join q.quiz quiz
            left join q.topic t
            where a.correct = false
            group by q.id, q.statement, t.id, t.name, quiz.topic.id, quiz.topic.name
            order by count(a.id) desc, q.statement asc
            """)
    Page<FrequentQuizError> findFrequentErrors(Pageable pageable);

    @Query("""
            select q.id as questionId,
                   q.statement as questionStatement,
                   coalesce(t.id, quiz.topic.id) as topicId,
                   coalesce(t.name, quiz.topic.name) as topicName,
                   count(a.id) as errorCount
            from QuizAnswer a
            join a.question q
            join q.quiz quiz
            left join q.topic t
            where a.correct = false and coalesce(t.id, quiz.topic.id) = :topicId
            group by q.id, q.statement, t.id, t.name, quiz.topic.id, quiz.topic.name
            order by count(a.id) desc, q.statement asc
            """)
    Page<FrequentQuizError> findFrequentErrorsByTopic(@Param("topicId") UUID topicId, Pageable pageable);
}
