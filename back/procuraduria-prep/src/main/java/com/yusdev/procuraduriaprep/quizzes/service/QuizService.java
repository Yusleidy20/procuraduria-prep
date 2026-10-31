package com.yusdev.procuraduriaprep.quizzes.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateMockExamRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizQuestionRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.CreateQuizRequest;
import com.yusdev.procuraduriaprep.quizzes.dto.QuizQuestionResponse;
import com.yusdev.procuraduriaprep.quizzes.dto.QuizResponse;
import com.yusdev.procuraduriaprep.quizzes.entity.Quiz;
import com.yusdev.procuraduriaprep.quizzes.entity.QuizQuestion;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizQuestionRepository;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizRepository;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;
import com.yusdev.procuraduriaprep.study.entity.Topic;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@Service
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final StudyDocumentRepository documentRepository;

    public QuizService(QuizRepository quizRepository, QuizQuestionRepository questionRepository,
                       TopicRepository topicRepository, StudyDocumentRepository documentRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public QuizResponse create(UUID topicId, CreateQuizRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Tema", topicId));
        for (CreateQuizQuestionRequest question : request.questions()) {
            if (question.correctOptionIndex() >= question.options().size()) {
                throw new InvalidRequestException("La opción correcta debe corresponder a una opción existente");
            }
        }

        Quiz quiz = quizRepository.save(new Quiz(request.name().trim(), normalize(request.description()),
                topic, request.difficulty(), request.durationMinutes()));
        List<QuizQuestion> questions = request.questions().stream()
                .map(question -> createQuestion(quiz, question))
                .toList();
        questionRepository.saveAll(questions);
        return QuizResponse.from(quiz, questions.size());
    }

    @Transactional
    public QuizResponse createMockExam(CreateMockExamRequest request) {
        Set<UUID> topicIds = Set.copyOf(request.topicIds());
        if (topicIds.size() != request.topicIds().size()) {
            throw new InvalidRequestException("No se permiten temas repetidos en un simulacro");
        }
        List<Topic> topics = topicRepository.findAllById(topicIds);
        if (topics.size() != topicIds.size()) {
            Set<UUID> foundIds = topics.stream().map(Topic::getId).collect(Collectors.toSet());
            Set<UUID> missingIds = topicIds.stream().filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Temas", missingIds);
        }

        List<QuizQuestion> candidates = questionRepository.findExamCandidates(
                List.copyOf(topicIds), request.difficulty(), PageRequest.of(0, 1000));
        if (candidates.size() < request.questionCount()) {
            throw new InvalidRequestException("No hay suficientes preguntas disponibles con esos temas y dificultad");
        }
        Collections.shuffle(candidates);
        List<QuizQuestion> selectedQuestions = new ArrayList<>(
                candidates.subList(0, request.questionCount()));

        Quiz exam = new Quiz(request.name().trim(), normalize(request.description()), topics.get(0),
                request.difficulty(), request.durationMinutes());
        exam.configureMockExam(topicIds);
        exam = quizRepository.save(exam);
        Quiz savedExam = exam;
        List<QuizQuestion> examQuestions = selectedQuestions.stream()
                .map(question -> new QuizQuestion(savedExam, question.getTopic(), question.getStatement(),
                        question.getOptions(), question.getCorrectOptionIndex(), question.getExplanation(),
                        question.getSourceDocument()))
                .toList();
        questionRepository.saveAll(examQuestions);
        return QuizResponse.from(exam, examQuestions.size());
    }

    public Page<QuizResponse> getAll(UUID topicId, Pageable pageable) {
        Page<Quiz> quizzes;
        if (topicId == null) {
            quizzes = quizRepository.findAll(pageable);
        } else {
            if (!topicRepository.existsById(topicId)) {
                throw new ResourceNotFoundException("Tema", topicId);
            }
            quizzes = quizRepository.findAllByAssociatedTopic(topicId, pageable);
        }
        return quizzes.map(quiz -> QuizResponse.from(quiz, questionRepository.countByQuizId(quiz.getId())));
    }

    public QuizResponse getById(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuestionario", quizId));
        return QuizResponse.from(quiz, questionRepository.countByQuizId(quizId));
    }

    public Page<QuizQuestionResponse> getQuestions(UUID quizId, Pageable pageable) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Cuestionario", quizId);
        }
        return questionRepository.findAllByQuizId(quizId, pageable).map(QuizQuestionResponse::from);
    }

    private QuizQuestion createQuestion(Quiz quiz, CreateQuizQuestionRequest request) {
        StudyDocument source = null;
        if (request.sourceDocumentId() != null) {
            source = documentRepository.findById(request.sourceDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Documento", request.sourceDocumentId()));
            if (!source.isActive()) {
                throw new ResourceNotFoundException("Documento activo", source.getId());
            }
        }
        return new QuizQuestion(quiz, request.statement().trim(),
                request.options().stream().map(String::trim).toList(), request.correctOptionIndex(),
                normalize(request.explanation()), source);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
