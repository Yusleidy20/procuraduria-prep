package com.yusdev.procuraduriaprep.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.ai.dto.SaveGeneratedFlashcardsRequest;
import com.yusdev.procuraduriaprep.ai.dto.SaveGeneratedQuizRequest;
import com.yusdev.procuraduriaprep.flashcards.dto.CreateFlashcardRequest;
import com.yusdev.procuraduriaprep.flashcards.dto.FlashcardResponse;
import com.yusdev.procuraduriaprep.flashcards.service.FlashcardService;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizQuestionRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.QuizResponse;
import com.yusdev.procuraduriaprep.quizzes.service.QuizService;

@Service
public class AiContentPersistenceService {

    private final FlashcardService flashcardService;
    private final QuizService quizService;

    public AiContentPersistenceService(FlashcardService flashcardService, QuizService quizService) {
        this.flashcardService = flashcardService;
        this.quizService = quizService;
    }

    @Transactional
    public List<FlashcardResponse> saveFlashcards(SaveGeneratedFlashcardsRequest request) {
        return request.flashcards().stream()
                .map(card -> flashcardService.create(request.topicId(),
                        new CreateFlashcardRequest(card.question(), card.answer(), card.explanation(),
                                card.difficulty(), request.sourceDocumentId())))
                .toList();
    }

    @Transactional
    public QuizResponse saveQuiz(SaveGeneratedQuizRequest request) {
        List<CreateQuizQuestionRequest> questions = request.questions().stream()
                .map(question -> new CreateQuizQuestionRequest(question.statement(), question.options(),
                        question.correctOptionIndex(), question.explanation(), request.sourceDocumentId()))
                .toList();
        return quizService.create(request.topicId(),
                new CreateQuizRequest(request.name(), request.description(), request.difficulty(), null, questions));
    }
}
