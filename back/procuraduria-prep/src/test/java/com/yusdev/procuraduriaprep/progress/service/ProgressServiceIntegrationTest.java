package com.yusdev.procuraduriaprep.progress.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.flashcards.dto.CreateFlashcardRequest;
import com.yusdev.procuraduriaprep.flashcards.dto.RecordReviewRequest;
import com.yusdev.procuraduriaprep.flashcards.service.FlashcardService;
import com.yusdev.procuraduriaprep.progress.service.ProgressService;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizQuestionRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.RecordQuizAnswerRequest;
import com.yusdev.procuraduriaprep.quizzes.service.QuizAttemptService;
import com.yusdev.procuraduriaprep.quizzes.service.QuizService;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.StudyModule;
import com.yusdev.procuraduriaprep.study.entity.Topic;
import com.yusdev.procuraduriaprep.study.repository.StudyModuleRepository;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProgressServiceIntegrationTest {

    @Autowired
    private StudyModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizAttemptService attemptService;

    @Autowired
    private FlashcardService flashcardService;

    @Autowired
    private ProgressService progressService;

    @Test
    void reportsTopicProgressFrequentErrorsAndDueReviews() {
        StudyModule module = moduleRepository.save(new StudyModule("Constitución", null, 1));
        Topic topic = topicRepository.save(new Topic("Derechos", null, module, DifficultyLevel.BASIC));
        var quiz = quizService.create(topic.getId(), new CreateQuizRequest(
                "Repaso", null, DifficultyLevel.BASIC,
                null, List.of(new CreateQuizQuestionRequest("¿Mecanismo de protección?",
                List.of("Tutela", "Acción popular"), 0, "La tutela protege derechos fundamentales.", null))));
        var question = quizService.getQuestions(quiz.id(), PageRequest.of(0, 10)).getContent().get(0);
        var attempt = attemptService.start(quiz.id());
        attemptService.recordAnswer(attempt.id(), new RecordQuizAnswerRequest(question.id(), 1));
        attemptService.finish(attempt.id());

        var card = flashcardService.create(topic.getId(),
                new CreateFlashcardRequest("¿Qué mecanismo?", "Tutela", null, DifficultyLevel.BASIC, null));
        var dueReviews = progressService.getDueReviews(PageRequest.of(0, 10));
        flashcardService.recordReview(card.id(), new RecordReviewRequest(false));

        var overall = progressService.getOverall();
        var topicProgress = progressService.getByTopic(topic.getId());
        var errors = progressService.getFrequentErrors(topic.getId(), PageRequest.of(0, 10));

        assertEquals(1, overall.completedQuizAttempts());
        assertEquals(1, overall.quizAnswers());
        assertEquals(0, overall.correctQuizAnswers());
        assertEquals(1, overall.flashcardReviews());
        assertEquals(0, overall.rememberedReviews());
        assertEquals("0.00", overall.masteryPercentage().toPlainString());
        assertEquals(1, topicProgress.quizAnswers());
        assertEquals(1, topicProgress.flashcardCount());
        assertEquals(1, errors.getTotalElements());
        assertEquals(1, errors.getContent().get(0).errorCount());
        assertEquals(1, dueReviews.getTotalElements());
    }
}
