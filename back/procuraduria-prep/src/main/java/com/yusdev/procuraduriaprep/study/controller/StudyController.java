package com.yusdev.procuraduriaprep.study.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.yusdev.procuraduriaprep.study.dto.CreateStudyModuleRequest;
import com.yusdev.procuraduriaprep.study.dto.CreateTopicRequest;
import com.yusdev.procuraduriaprep.study.dto.StudyModuleResponse;
import com.yusdev.procuraduriaprep.study.dto.TopicResponse;
import com.yusdev.procuraduriaprep.study.service.StudyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/study")
public class StudyController {

	private final StudyService studyService;

	public StudyController(StudyService studyService) {
		this.studyService = studyService;
	}

	@PostMapping("/modules")
	public ResponseEntity<StudyModuleResponse> createModule(
			@Valid @RequestBody CreateStudyModuleRequest request,
			UriComponentsBuilder uriBuilder) {
		StudyModuleResponse response = studyService.createModule(request);
		URI location = uriBuilder.path("/api/v1/study/modules/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping("/modules")
	public Page<StudyModuleResponse> getModules(@PageableDefault(size = 20, sort = "displayOrder") Pageable pageable) {
		return studyService.getModules(pageable);
	}

	@GetMapping("/modules/{moduleId}")
	public StudyModuleResponse getModule(@PathVariable UUID moduleId) {
		return studyService.getModule(moduleId);
	}

	@PostMapping("/modules/{moduleId}/topics")
	public ResponseEntity<TopicResponse> createTopic(
			@PathVariable UUID moduleId,
			@Valid @RequestBody CreateTopicRequest request,
			UriComponentsBuilder uriBuilder) {
		TopicResponse response = studyService.createTopic(moduleId, request);
		URI location = uriBuilder.path("/api/v1/study/topics/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping("/modules/{moduleId}/topics")
	public Page<TopicResponse> getTopics(
			@PathVariable UUID moduleId,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return studyService.getTopics(moduleId, pageable);
	}

	@GetMapping("/topics/{topicId}")
	public TopicResponse getTopic(@PathVariable UUID topicId) {
		return studyService.getTopic(topicId);
	}
}
