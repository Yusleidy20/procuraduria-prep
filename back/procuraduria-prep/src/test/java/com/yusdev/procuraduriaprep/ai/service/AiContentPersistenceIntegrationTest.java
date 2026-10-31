package com.yusdev.procuraduriaprep.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.ai.dto.GeneratedFlashcard;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedQuizQuestion;
import com.yusdev.procuraduriaprep.ai.dto.SaveGeneratedFlashcardsRequest;
import com.yusdev.procuraduriaprep.ai.dto.SaveGeneratedQuizRequest;
import com.yusdev.procuraduriaprep.flashcards.service.FlashcardService;
import com.yusdev.procuraduriaprep.quizzes.service.QuizService;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.StudyModule;
import com.yusdev.procuraduriaprep.study.entity.Topic;
import com.yusdev.procuraduriaprep.study.repository.StudyModuleRepository;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiContentPersistenceIntegrationTest {

    @Autowired
    private StudyModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private AiContentPersistenceService contentPersistenceService;

    @Autowired
    private FlashcardService flashcardService;

    @Autowired
    private QuizService quizService;

    @Test
    void persistsGeneratedFlashcardsAndQuestionsAsStudyContent() {
        StudyModule module = moduleRepository.save(new StudyModule("Constitución", null, 1));
        Topic topic = topicRepository.save(new Topic("Derechos", null, module, DifficultyLevel.BASIC));

        var savedCards = contentPersistenceService.saveFlashcards(new SaveGeneratedFlashcardsRequest(topic.getId(),
                null, List.of(new GeneratedFlashcard("Pregunta", "Respuesta", "Explicación",
                DifficultyLevel.BASIC))));
        var cardsInTopic = flashcardService.getByTopic(topic.getId(), PageRequest.of(0, 10));

        assertEquals(1, savedCards.size());
        assertEquals(1, cardsInTopic.getTotalElements());
        assertEquals("Respuesta", cardsInTopic.getContent().get(0).answer());

        var savedQuiz = contentPersistenceService.saveQuiz(new SaveGeneratedQuizRequest(topic.getId(),
                null, "Cuestionario generado", null, DifficultyLevel.BASIC,
                List.of(new GeneratedQuizQuestion("Enunciado", List.of("A", "B"), 1, "Razón"))));
        var questions = quizService.getQuestions(savedQuiz.id(), PageRequest.of(0, 10));

        assertEquals(1, savedQuiz.questionCount());
        assertEquals(1, questions.getTotalElements());
        assertEquals("Enunciado", questions.getContent().get(0).statement());
    }
}
