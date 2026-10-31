package com.yusdev.procuraduriaprep.study.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.shared.exception.DuplicateResourceException;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;
import com.yusdev.procuraduriaprep.study.dto.CreateStudyModuleRequest;
import com.yusdev.procuraduriaprep.study.dto.CreateTopicRequest;
import com.yusdev.procuraduriaprep.study.dto.StudyModuleResponse;
import com.yusdev.procuraduriaprep.study.dto.TopicResponse;
import com.yusdev.procuraduriaprep.study.entity.StudyModule;
import com.yusdev.procuraduriaprep.study.entity.Topic;
import com.yusdev.procuraduriaprep.study.repository.StudyModuleRepository;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@Service
@Transactional(readOnly = true)
public class StudyService {

	private final StudyModuleRepository moduleRepository;
	private final TopicRepository topicRepository;

	public StudyService(StudyModuleRepository moduleRepository, TopicRepository topicRepository) {
		this.moduleRepository = moduleRepository;
		this.topicRepository = topicRepository;
	}

	@Transactional
	public StudyModuleResponse createModule(CreateStudyModuleRequest request) {
		if (moduleRepository.existsByNameIgnoreCase(request.name().trim())) {
			throw new DuplicateResourceException("Ya existe un módulo con ese nombre");
		}
		StudyModule module = moduleRepository.save(
				new StudyModule(request.name().trim(), normalize(request.description()), request.displayOrder()));
		return StudyModuleResponse.from(module);
	}

	public Page<StudyModuleResponse> getModules(Pageable pageable) {
		return moduleRepository.findAll(pageable).map(StudyModuleResponse::from);
	}

	public StudyModuleResponse getModule(UUID moduleId) {
		StudyModule module = moduleRepository.findById(moduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Módulo", moduleId));
		return StudyModuleResponse.from(module);
	}

	@Transactional
	public TopicResponse createTopic(UUID moduleId, CreateTopicRequest request) {
		StudyModule module = moduleRepository.findById(moduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Módulo", moduleId));
		if (topicRepository.existsByModuleIdAndNameIgnoreCase(moduleId, request.name().trim())) {
			throw new DuplicateResourceException("Ya existe un tema con ese nombre en el módulo");
		}
		Topic topic = topicRepository.save(new Topic(request.name().trim(), normalize(request.description()),
				module, request.difficulty()));
		return TopicResponse.from(topic);
	}

	public Page<TopicResponse> getTopics(UUID moduleId, Pageable pageable) {
		if (!moduleRepository.existsById(moduleId)) {
			throw new ResourceNotFoundException("Módulo", moduleId);
		}
		return topicRepository.findAllByModuleId(moduleId, pageable).map(TopicResponse::from);
	}

	public TopicResponse getTopic(UUID topicId) {
		Topic topic = topicRepository.findById(topicId)
				.orElseThrow(() -> new ResourceNotFoundException("Tema", topicId));
		return TopicResponse.from(topic);
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
