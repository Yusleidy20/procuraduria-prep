package com.yusdev.procuraduriaprep.quizzes.controller;

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

import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateMockExamRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.QuizQuestionResponse;
import com.yusdev.procuraduriaprep.quizzes.dto.QuizResponse;
import com.yusdev.procuraduriaprep.quizzes.service.QuizService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping
    public ResponseEntity<QuizResponse> create(
            @RequestParam @NotNull UUID topicId,
            @Valid @RequestBody CreateQuizRequest request,
            UriComponentsBuilder uriBuilder) {
        QuizResponse response = quizService.create(topicId, request);
        URI location = uriBuilder.path("/api/v1/quizzes/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/mock-exams")
    public ResponseEntity<QuizResponse> createMockExam(
            @Valid @RequestBody CreateMockExamRequest request,
            UriComponentsBuilder uriBuilder) {
        QuizResponse response = quizService.createMockExam(request);
        URI location = uriBuilder.path("/api/v1/quizzes/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public Page<QuizResponse> getAll(
            @RequestParam(required = false) UUID topicId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return quizService.getAll(topicId, pageable);
    }

    @GetMapping("/{quizId}")
    public QuizResponse getById(@PathVariable UUID quizId) {
        return quizService.getById(quizId);
    }

    @GetMapping("/{quizId}/questions")
    public Page<QuizQuestionResponse> getQuestions(
            @PathVariable UUID quizId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return quizService.getQuestions(quizId, pageable);
    }
}
