package com.yusdev.procuraduriaprep.quizzes.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.study.entity.Topic;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false, length = 3000)
    private String statement;

    @ElementCollection
    @CollectionTable(name = "quiz_question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_order")
    @Column(name = "option_text", nullable = false, length = 500)
    private List<String> options = new ArrayList<>();

    @Column(name = "correct_option_index", nullable = false)
    private int correctOptionIndex;

    @Column(length = 3000)
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private StudyDocument sourceDocument;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected QuizQuestion() {
    }

    public QuizQuestion(Quiz quiz, String statement, List<String> options, int correctOptionIndex,
                        String explanation, StudyDocument sourceDocument) {
        this(quiz, quiz.getTopic(), statement, options, correctOptionIndex, explanation, sourceDocument);
    }

    public QuizQuestion(Quiz quiz, Topic topic, String statement, List<String> options, int correctOptionIndex,
                        String explanation, StudyDocument sourceDocument) {
        this.quiz = quiz;
        this.topic = topic;
        this.statement = statement;
        this.options = new ArrayList<>(options);
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
        this.sourceDocument = sourceDocument;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public Topic getTopic() {
        return topic == null ? quiz.getTopic() : topic;
    }

    public String getStatement() {
        return statement;
    }

    public List<String> getOptions() {
        return List.copyOf(options);
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public StudyDocument getSourceDocument() {
        return sourceDocument;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
