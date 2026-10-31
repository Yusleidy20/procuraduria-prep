package com.yusdev.procuraduriaprep.ai.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.dto.GenerateStudyContentRequest;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedFlashcard;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedQuizQuestion;
import com.yusdev.procuraduriaprep.ai.dto.SaveGeneratedFlashcardsRequest;
import com.yusdev.procuraduriaprep.ai.dto.SaveGeneratedQuizRequest;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;
import com.yusdev.procuraduriaprep.ai.service.AiContentPersistenceService;
import com.yusdev.procuraduriaprep.ai.service.AiTutorService;
import com.yusdev.procuraduriaprep.flashcards.dto.FlashcardResponse;
import com.yusdev.procuraduriaprep.quizzes.dto.QuizResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ai")
public class AiTutorController {

    private final AiTutorService aiTutorService;
    private final AiContentPersistenceService contentPersistenceService;

    public AiTutorController(AiTutorService aiTutorService, AiContentPersistenceService contentPersistenceService) {
        this.aiTutorService = aiTutorService;
        this.contentPersistenceService = contentPersistenceService;
    }

    @PostMapping("/explanations")
    public TopicExplanationResponse explain(@Valid @RequestBody ExplainTopicRequest request) {
        return aiTutorService.explainTopic(request);
    }

    @PostMapping("/flashcards")
    public List<GeneratedFlashcard> generateFlashcards(
            @Valid @RequestBody GenerateStudyContentRequest request) {
        return aiTutorService.generateFlashcards(request);
    }

    @PostMapping("/flashcards/save")
    public List<FlashcardResponse> saveFlashcards(@Valid @RequestBody SaveGeneratedFlashcardsRequest request) {
        return contentPersistenceService.saveFlashcards(request);
    }

    @PostMapping("/questions")
    public List<GeneratedQuizQuestion> generateQuestions(
            @Valid @RequestBody GenerateStudyContentRequest request) {
        return aiTutorService.generateQuestions(request);
    }

    @PostMapping("/questions/save")
    public QuizResponse saveQuiz(@Valid @RequestBody SaveGeneratedQuizRequest request) {
        return contentPersistenceService.saveQuiz(request);
    }
}
