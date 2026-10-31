package com.yusdev.procuraduriaprep.progress.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yusdev.procuraduriaprep.flashcards.dto.FlashcardResponse;
import com.yusdev.procuraduriaprep.progress.dto.FrequentErrorResponse;
import com.yusdev.procuraduriaprep.progress.dto.OverallProgressResponse;
import com.yusdev.procuraduriaprep.progress.dto.TopicProgressResponse;
import com.yusdev.procuraduriaprep.progress.service.ProgressService;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

	private final ProgressService progressService;

	public ProgressController(ProgressService progressService) {
		this.progressService = progressService;
	}

	@GetMapping
	public OverallProgressResponse getOverall() {
		return progressService.getOverall();
	}

	@GetMapping("/topics/{topicId}")
	public TopicProgressResponse getByTopic(@PathVariable UUID topicId) {
		return progressService.getByTopic(topicId);
	}

	@GetMapping("/errors")
	public Page<FrequentErrorResponse> getFrequentErrors(
			@RequestParam(required = false) UUID topicId,
			@PageableDefault(size = 20) Pageable pageable) {
		return progressService.getFrequentErrors(topicId, pageable);
	}

	@GetMapping("/reviews/due")
	public Page<FlashcardResponse> getDueReviews(@PageableDefault(size = 20) Pageable pageable) {
		return progressService.getDueReviews(pageable);
	}
}
