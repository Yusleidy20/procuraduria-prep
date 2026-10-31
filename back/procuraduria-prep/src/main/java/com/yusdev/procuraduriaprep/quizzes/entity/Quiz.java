package com.yusdev.procuraduriaprep.quizzes.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.Topic;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @ElementCollection
    @CollectionTable(name = "quiz_topics", joinColumns = @JoinColumn(name = "quiz_id"))
    @Column(name = "topic_id", nullable = false)
    private Set<UUID> topicIds = new HashSet<>();

    @Column(name = "mock_exam")
    private Boolean mockExam;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Quiz() {
    }

    public Quiz(String name, String description, Topic topic, DifficultyLevel difficulty, Integer durationMinutes) {
        this.name = name;
        this.description = description;
        this.topic = topic;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.topicIds.add(topic.getId());
        this.createdAt = Instant.now();
    }

    public void configureMockExam(Set<UUID> topicIds) {
        this.topicIds.clear();
        this.topicIds.addAll(topicIds);
        this.mockExam = true;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Topic getTopic() {
        return topic;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Set<UUID> getTopicIds() {
        if (topicIds.isEmpty() && topic.getId() != null) {
            return Set.of(topic.getId());
        }
        return Set.copyOf(topicIds);
    }

    public boolean isMockExam() {
        return Boolean.TRUE.equals(mockExam);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
