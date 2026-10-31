package com.yusdev.procuraduriaprep.flashcards.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.yusdev.procuraduriaprep.flashcards.dto.CreateFlashcardRequest;
import com.yusdev.procuraduriaprep.flashcards.dto.FlashcardResponse;
import com.yusdev.procuraduriaprep.flashcards.dto.RecordReviewRequest;
import com.yusdev.procuraduriaprep.flashcards.service.FlashcardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/flashcards")
public class FlashcardController {

	private final FlashcardService flashcardService;

	public FlashcardController(FlashcardService flashcardService) {
		this.flashcardService = flashcardService;
	}

	@PostMapping
	public ResponseEntity<FlashcardResponse> create(
			@RequestParam UUID topicId,
			@Valid @RequestBody CreateFlashcardRequest request,
			UriComponentsBuilder uriBuilder) {
		FlashcardResponse response = flashcardService.create(topicId, request);
		URI location = uriBuilder.path("/api/v1/flashcards/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public Page<FlashcardResponse> getByTopic(
			@RequestParam UUID topicId,
			@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
		return flashcardService.getByTopic(topicId, pageable);
	}

	@GetMapping("/{flashcardId}")
	public FlashcardResponse getById(@PathVariable UUID flashcardId) {
		return flashcardService.getById(flashcardId);
	}

	@GetMapping("/due")
	public Page<FlashcardResponse> getDue(@PageableDefault(size = 20, sort = "nextReviewAt") Pageable pageable) {
		return flashcardService.getDue(pageable);
	}

	@PostMapping("/{flashcardId}/reviews")
	public FlashcardResponse recordReview(
			@PathVariable UUID flashcardId,
			@Valid @RequestBody RecordReviewRequest request) {
		return flashcardService.recordReview(flashcardId, request);
	}
}
