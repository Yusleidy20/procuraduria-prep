package com.yusdev.procuraduriaprep.study.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "study_topics", uniqueConstraints = @UniqueConstraint(
		name = "uk_topic_module_name", columnNames = { "module_id", "name" }))
public class Topic {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(length = 2000)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "module_id", nullable = false)
	private StudyModule module;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DifficultyLevel difficulty;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Topic() {
	}

	public Topic(String name, String description, StudyModule module, DifficultyLevel difficulty) {
		this.name = name;
		this.description = description;
		this.module = module;
		this.difficulty = difficulty;
		this.createdAt = Instant.now();
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

	public StudyModule getModule() {
		return module;
	}

	public DifficultyLevel getDifficulty() {
		return difficulty;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
