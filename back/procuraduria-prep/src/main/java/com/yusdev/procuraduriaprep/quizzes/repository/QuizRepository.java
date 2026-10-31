package com.yusdev.procuraduriaprep.quizzes.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yusdev.procuraduriaprep.quizzes.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Page<Quiz> findAllByTopicId(UUID topicId, Pageable pageable);

    @Query(value = """
            select q from Quiz q
            where q.topic.id = :topicId or :topicId member of q.topicIds
            """,
            countQuery = """
                    select count(q) from Quiz q
                    where q.topic.id = :topicId or :topicId member of q.topicIds
                    """)
    Page<Quiz> findAllByAssociatedTopic(@Param("topicId") UUID topicId, Pageable pageable);
}
