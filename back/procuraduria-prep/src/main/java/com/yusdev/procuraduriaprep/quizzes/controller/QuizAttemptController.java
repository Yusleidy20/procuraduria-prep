package com.yusdev.procuraduriaprep.quizzes.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.yusdev.procuraduriaprep.quizzes.dto.QuizAttemptResponse;
import com.yusdev.procuraduriaprep.quizzes.dto.RecordQuizAnswerRequest;
import com.yusdev.procuraduriaprep.quizzes.service.QuizAttemptService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class QuizAttemptController {

	private final QuizAttemptService attemptService;

	public QuizAttemptController(QuizAttemptService attemptService) {
		this.attemptService = attemptService;
	}

	@PostMapping("/quizzes/{quizId}/attempts")
	public ResponseEntity<QuizAttemptResponse> start(
			@PathVariable UUID quizId,
			UriComponentsBuilder uriBuilder) {
		QuizAttemptResponse response = attemptService.start(quizId);
		URI location = uriBuilder.path("/api/v1/quiz-attempts/{attemptId}")
				.buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(location).body(response);
	}

	@PostMapping("/quiz-attempts/{attemptId}/answers")
	public ResponseEntity<Void> recordAnswer(
			@PathVariable UUID attemptId,
			@Valid @RequestBody RecordQuizAnswerRequest request) {
		attemptService.recordAnswer(attemptId, request);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/quiz-attempts/{attemptId}/finish")
	public QuizAttemptResponse finish(@PathVariable UUID attemptId) {
		return attemptService.finish(attemptId);
	}

	@GetMapping("/quiz-attempts/{attemptId}")
	public QuizAttemptResponse getResult(@PathVariable UUID attemptId) {
		return attemptService.getAttemptResult(attemptId);
	}
}
