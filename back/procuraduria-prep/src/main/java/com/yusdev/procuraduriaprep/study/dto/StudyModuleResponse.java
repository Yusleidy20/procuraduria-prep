package com.yusdev.procuraduriaprep.study.dto;

import java.time.Instant;
import java.util.UUID;

import com.yusdev.procuraduriaprep.study.entity.StudyModule;
import com.yusdev.procuraduriaprep.study.entity.StudyModuleStatus;

public record StudyModuleResponse(
		UUID id,
		String name,
		String description,
		int displayOrder,
		StudyModuleStatus status,
		Instant createdAt) {

	public static StudyModuleResponse from(StudyModule module) {
		return new StudyModuleResponse(module.getId(), module.getName(), module.getDescription(),
				module.getDisplayOrder(), module.getStatus(), module.getCreatedAt());
	}
}
