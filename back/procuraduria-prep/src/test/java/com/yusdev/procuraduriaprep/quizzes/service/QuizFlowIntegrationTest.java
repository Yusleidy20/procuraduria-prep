package com.yusdev.procuraduriaprep.quizzes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizQuestionRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateMockExamRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.RecordQuizAnswerRequest;
import com.yusdev.procuraduriaprep.quizzes.entity.QuizAttempt;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizAttemptRepository;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.progress.service.ProgressService;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.StudyModule;
import com.yusdev.procuraduriaprep.study.entity.Topic;
import com.yusdev.procuraduriaprep.study.repository.StudyModuleRepository;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuizFlowIntegrationTest {

    @Autowired
    private StudyModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizAttemptService attemptService;

    @Autowired
    private QuizAttemptRepository attemptRepository;

    @Autowired
    private ProgressService progressService;

    @Test
    void createsQuizHidesAnswerUntilFinishAndCalculatesResult() {
        StudyModule module = moduleRepository.save(new StudyModule("Constitución", null, 1));
        Topic topic = topicRepository.save(new Topic("Derechos", null, module, DifficultyLevel.BASIC));
        var quiz = quizService.create(topic.getId(), new CreateQuizRequest(
                "Repaso", null, DifficultyLevel.BASIC,
                null, List.of(new CreateQuizQuestionRequest(
                "¿Qué mecanismo protege derechos fundamentales?",
                List.of("Tutela", "Acción popular"), 0, "Explicación", null))));

        var questions = quizService.getQuestions(quiz.id(), PageRequest.of(0, 10));
        assertEquals(1, questions.getTotalElements());
        assertFalse(questions.getContent().get(0).toString().contains("correctOptionIndex"));

        var attempt = attemptService.start(quiz.id());
        attemptService.recordAnswer(attempt.id(),
                new RecordQuizAnswerRequest(questions.getContent().get(0).id(), 0));
        var result = attemptService.finish(attempt.id());

        assertEquals(1, result.correctAnswers());
        assertEquals(0, result.incorrectAnswers());
        assertEquals("100.00", result.scorePercentage().toPlainString());
        assertEquals("Explicación", result.answers().get(0).explanation());
    }

    @Test
    void expiresTimedQuizAttemptAndPreventsLateAnswers() throws ReflectiveOperationException {
        StudyModule module = moduleRepository.save(new StudyModule("Constitución", null, 2));
        Topic topic = topicRepository.save(new Topic("Acciones", null, module, DifficultyLevel.BASIC));
        var quiz = quizService.create(topic.getId(), new CreateQuizRequest(
                "Simulacro", null, DifficultyLevel.BASIC, 1,
                List.of(new CreateQuizQuestionRequest("Pregunta", List.of("A", "B"), 0, "Explicación", null))));

        var questions = quizService.getQuestions(quiz.id(), PageRequest.of(0, 10));
        var started = attemptService.start(quiz.id());
        assertNotNull(started.deadlineAt());
        QuizAttempt attempt = attemptRepository.findById(started.id()).orElseThrow();
        Field startedAt = QuizAttempt.class.getDeclaredField("startedAt");
        startedAt.setAccessible(true);
        startedAt.set(attempt, Instant.now().minus(Duration.ofMinutes(2)));

        assertThrows(InvalidRequestException.class, () -> attemptService.recordAnswer(started.id(),
                new RecordQuizAnswerRequest(questions.getContent().get(0).id(), 0)));
        var result = attemptService.getAttemptResult(started.id());

        assertNotNull(result.finishedAt());
        assertEquals(0, result.correctAnswers());
        assertEquals(1, result.incorrectAnswers());
        assertEquals("0.00", result.scorePercentage().toPlainString());
    }

    @Test
    void createsMixedTopicMockExamAndTracksProgressForEachTopic() {
        StudyModule module = moduleRepository.save(new StudyModule("Constitución", null, 3));
        Topic firstTopic = topicRepository.save(new Topic("Derechos", null, module, DifficultyLevel.BASIC));
        Topic secondTopic = topicRepository.save(new Topic("Acciones", null, module, DifficultyLevel.BASIC));
        quizService.create(firstTopic.getId(), new CreateQuizRequest(
                "Banco de derechos", null, DifficultyLevel.BASIC, null,
                List.of(new CreateQuizQuestionRequest("Pregunta de derechos", List.of("A", "B"),
                        0, "Explicación", null))));
        quizService.create(secondTopic.getId(), new CreateQuizRequest(
                "Banco de acciones", null, DifficultyLevel.BASIC, null,
                List.of(new CreateQuizQuestionRequest("Pregunta de acciones", List.of("C", "D"),
                        1, "Explicación", null))));

        var exam = quizService.createMockExam(new CreateMockExamRequest("Simulacro mixto", null,
                List.of(firstTopic.getId(), secondTopic.getId()), DifficultyLevel.BASIC, 2, 30));
        assertEquals(2, exam.questionCount());
        assertEquals(2, exam.topicIds().size());
        assertTrue(exam.mockExam());
        assertEquals(1, quizService.getAll(secondTopic.getId(), PageRequest.of(0, 10))
                .getContent().stream().filter(quiz -> quiz.id().equals(exam.id())).count());

        var questions = quizService.getQuestions(exam.id(), PageRequest.of(0, 10));
        assertEquals(2, questions.getTotalElements());
        var attempt = attemptService.start(exam.id());
        for (var question : questions.getContent()) {
            attemptService.recordAnswer(attempt.id(), new RecordQuizAnswerRequest(question.id(), 0));
        }
        attemptService.finish(attempt.id());

        assertEquals(2, attemptService.getAttemptResult(attempt.id()).totalQuestions());
        assertEquals(1, progressService.getByTopic(firstTopic.getId()).quizAnswers());
        assertEquals(1, progressService.getByTopic(secondTopic.getId()).quizAnswers());
        assertEquals(1, progressService.getByTopic(firstTopic.getId()).completedQuizAttempts());
        assertEquals(1, progressService.getByTopic(secondTopic.getId()).completedQuizAttempts());
    }
}
