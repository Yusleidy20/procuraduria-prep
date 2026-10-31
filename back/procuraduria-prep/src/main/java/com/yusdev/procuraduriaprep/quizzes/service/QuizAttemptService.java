package com.yusdev.procuraduriaprep.quizzes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.quizzes.dto.QuizAnswerResultResponse;
import com.yusdev.procuraduriaprep.quizzes.dto.QuizAttemptResponse;
import com.yusdev.procuraduriaprep.quizzes.dto.RecordQuizAnswerRequest;
import com.yusdev.procuraduriaprep.quizzes.entity.Quiz;
import com.yusdev.procuraduriaprep.quizzes.entity.QuizAnswer;
import com.yusdev.procuraduriaprep.quizzes.entity.QuizAttempt;
import com.yusdev.procuraduriaprep.quizzes.entity.QuizQuestion;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizAnswerRepository;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizAttemptRepository;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizQuestionRepository;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizRepository;
import com.yusdev.procuraduriaprep.shared.exception.DuplicateResourceException;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;

    public QuizAttemptService(QuizRepository quizRepository, QuizQuestionRepository questionRepository,
                              QuizAttemptRepository attemptRepository, QuizAnswerRepository answerRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional
    public QuizAttemptResponse start(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuestionario", quizId));
        int questionCount = Math.toIntExact(questionRepository.countByQuizId(quizId));
        if (questionCount == 0) {
            throw new InvalidRequestException("No se puede iniciar un cuestionario sin preguntas");
        }
        QuizAttempt attempt = attemptRepository.save(new QuizAttempt(quiz, questionCount));
        return QuizAttemptResponse.from(attempt, List.of());
    }

    @Transactional
    public void recordAnswer(UUID attemptId, RecordQuizAnswerRequest request) {
        QuizAttempt attempt = getAttempt(attemptId);
        if (attempt.isFinished()) {
            throw new InvalidRequestException("El intento ya fue finalizado");
        }
        if (attempt.isExpired(Instant.now())) {
            throw new InvalidRequestException("El tiempo del intento terminó; finalízalo para consultar el resultado");
        }
        QuizQuestion question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta", request.questionId()));
        if (!question.getQuiz().getId().equals(attempt.getQuiz().getId())) {
            throw new InvalidRequestException("La pregunta no pertenece al cuestionario de este intento");
        }
        if (request.selectedOptionIndex() >= question.getOptions().size()) {
            throw new InvalidRequestException("La opción seleccionada no existe para esta pregunta");
        }
        if (answerRepository.existsByAttemptIdAndQuestionId(attemptId, question.getId())) {
            throw new DuplicateResourceException("Ya se registró una respuesta para esta pregunta en el intento");
        }
        answerRepository.save(new QuizAnswer(attempt, question, request.selectedOptionIndex(), Instant.now()));
    }

    @Transactional
    public QuizAttemptResponse finish(UUID attemptId) {
        QuizAttempt attempt = getAttempt(attemptId);
        if (attempt.isFinished()) {
            throw new InvalidRequestException("El intento ya fue finalizado");
        }
        return finishAttempt(attempt, attemptId);
    }

    @Transactional
    public QuizAttemptResponse getAttemptResult(UUID attemptId) {
        QuizAttempt attempt = getAttempt(attemptId);
        if (!attempt.isFinished() && attempt.isExpired(Instant.now())) {
            return finishAttempt(attempt, attemptId);
        }
        List<QuizAnswerResultResponse> answers = attempt.isFinished()
                ? answerRepository.findAllByAttemptIdOrderByAnsweredAtAsc(attemptId).stream()
                .map(QuizAnswerResultResponse::from).toList()
                : List.of();
        return QuizAttemptResponse.from(attempt, answers);
    }

    private QuizAttemptResponse finishAttempt(QuizAttempt attempt, UUID attemptId) {
        int correctAnswers = Math.toIntExact(answerRepository.countByAttemptIdAndCorrectTrue(attemptId));
        int incorrectAnswers = attempt.getTotalQuestions() - correctAnswers;
        BigDecimal score = BigDecimal.valueOf(correctAnswers)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(attempt.getTotalQuestions()), 2, RoundingMode.HALF_UP);
        attempt.finish(Instant.now(), correctAnswers, incorrectAnswers, score);
        List<QuizAnswerResultResponse> answers = answerRepository.findAllByAttemptIdOrderByAnsweredAtAsc(attemptId)
                .stream().map(QuizAnswerResultResponse::from).toList();
        return QuizAttemptResponse.from(attempt, answers);
    }

    private QuizAttempt getAttempt(UUID attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", attemptId));
    }
}
