package com.yusdev.procuraduriaprep.quizzes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yusdev.procuraduriaprep.quizzes.dto.RecordQuizAnswerRequest;
import com.yusdev.procuraduriaprep.quizzes.entity.Quiz;
import com.yusdev.procuraduriaprep.quizzes.entity.QuizAttempt;
import com.yusdev.procuraduriaprep.quizzes.entity.QuizQuestion;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizAnswerRepository;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizAttemptRepository;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizQuestionRepository;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizRepository;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.StudyModule;
import com.yusdev.procuraduriaprep.study.entity.Topic;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

	@Mock
	private QuizRepository quizRepository;

	@Mock
	private QuizQuestionRepository questionRepository;

	@Mock
	private QuizAttemptRepository attemptRepository;

	@Mock
	private QuizAnswerRepository answerRepository;

	@InjectMocks
	private QuizAttemptService attemptService;

	@Test
	void finishesAttemptAndCalculatesPercentageAcrossAllQuizQuestions() {
		Quiz quiz = quiz();
		QuizAttempt attempt = new QuizAttempt(quiz, 2);
		UUID attemptId = UUID.randomUUID();
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(answerRepository.countByAttemptIdAndCorrectTrue(attemptId)).thenReturn(1L);
		when(answerRepository.findAllByAttemptIdOrderByAnsweredAtAsc(attemptId)).thenReturn(List.of());

		var response = attemptService.finish(attemptId);

		assertEquals(1, response.correctAnswers());
		assertEquals(1, response.incorrectAnswers());
		assertEquals("50.00", response.scorePercentage().toPlainString());
		verify(attemptRepository).findById(attemptId);
	}

	@Test
	void rejectsAnswerForQuestionFromDifferentQuiz() {
		UUID attemptQuizId = UUID.randomUUID();
		UUID foreignQuizId = UUID.randomUUID();
		Quiz attemptQuiz = mock(Quiz.class);
		Quiz foreignQuiz = mock(Quiz.class);
		when(attemptQuiz.getId()).thenReturn(attemptQuizId);
		when(foreignQuiz.getId()).thenReturn(foreignQuizId);
		QuizAttempt attempt = new QuizAttempt(attemptQuiz, 1);
		when(attemptQuiz.getDurationMinutes()).thenReturn(null);
		QuizQuestion foreignQuestion = new QuizQuestion(foreignQuiz, "Pregunta", List.of("A", "B"), 0, null, null);
		UUID attemptId = UUID.randomUUID();
		UUID questionId = UUID.randomUUID();
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(questionRepository.findById(questionId)).thenReturn(Optional.of(foreignQuestion));

		assertThrows(InvalidRequestException.class,
				() -> attemptService.recordAnswer(attemptId, new RecordQuizAnswerRequest(questionId, 0)));
		org.mockito.Mockito.verify(answerRepository, org.mockito.Mockito.never()).save(any());
	}

	private Quiz quiz() {
		Topic topic = new Topic("Derechos", null,
				new StudyModule("Constitución", null, 0), DifficultyLevel.BASIC);
		return new Quiz("Repaso", null, topic, DifficultyLevel.BASIC, null);
	}
}
