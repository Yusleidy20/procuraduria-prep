package com.yusdev.procuraduriaprep.studychat.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.yusdev.procuraduriaprep.users.entity.AppUser;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_conversations")
public class StudyConversation {

    public static final String DEFAULT_TITLE = "Nueva conversación";

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyMessage> messages = new ArrayList<>();

    protected StudyConversation() {
    }

    public StudyConversation(AppUser user, String title) {
        this.user = user;
        this.title = title == null || title.isBlank() ? DEFAULT_TITLE : title.strip();
    }

    public void rename(String title) {
        this.title = title.strip();
        touch();
    }

    public void setInitialTitle(String title) {
        if (DEFAULT_TITLE.equals(this.title)) {
            this.title = title.strip();
            touch();
        }
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void addMessage(StudyMessage message) {
        messages.add(message);
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
