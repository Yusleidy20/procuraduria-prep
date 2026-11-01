package com.yusdev.procuraduriaprep.studychat.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_messages")
public class StudyMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private StudyConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyMessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, columnDefinition = "text")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StudyMessage() {
    }

    public StudyMessage(StudyConversation conversation, StudyMessageRole role, String content, String metadata) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.metadata = metadata == null ? "{}" : metadata;
    }

    @PrePersist
    void initializeTimestamp() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public StudyConversation getConversation() { return conversation; }
    public StudyMessageRole getRole() { return role; }
    public String getContent() { return content; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
