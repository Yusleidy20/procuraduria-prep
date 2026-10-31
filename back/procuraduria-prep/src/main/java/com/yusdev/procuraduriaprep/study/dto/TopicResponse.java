package com.yusdev.procuraduriaprep.study.dto;

import java.time.Instant;
import java.util.UUID;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.Topic;

public record TopicResponse(
		UUID id,
		String name,
		String description,
		UUID moduleId,
		String moduleName,
		DifficultyLevel difficulty,
		Instant createdAt) {

	public static TopicResponse from(Topic topic) {
		return new TopicResponse(topic.getId(), topic.getName(), topic.getDescription(),
				topic.getModule().getId(), topic.getModule().getName(), topic.getDifficulty(), topic.getCreatedAt());
	}
}
