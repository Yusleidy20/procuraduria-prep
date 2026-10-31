package com.yusdev.procuraduriaprep.progress.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.flashcards.dto.FlashcardResponse;
import com.yusdev.procuraduriaprep.flashcards.repository.FlashcardRepository;
import com.yusdev.procuraduriaprep.flashcards.repository.FlashcardReviewRepository;
import com.yusdev.procuraduriaprep.progress.dto.FrequentErrorResponse;
import com.yusdev.procuraduriaprep.progress.dto.OverallProgressResponse;
import com.yusdev.procuraduriaprep.progress.dto.TopicProgressResponse;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizAnswerRepository;
import com.yusdev.procuraduriaprep.quizzes.repository.QuizAttemptRepository;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@Service
@Transactional(readOnly = true)
public class ProgressService {

    private final TopicRepository topicRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardReviewRepository reviewRepository;

    public ProgressService(TopicRepository topicRepository, QuizAttemptRepository attemptRepository,
                           QuizAnswerRepository answerRepository, FlashcardRepository flashcardRepository,
                           FlashcardReviewRepository reviewRepository) {
        this.topicRepository = topicRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.flashcardRepository = flashcardRepository;
        this.reviewRepository = reviewRepository;
    }

    public OverallProgressResponse getOverall() {
        long quizAnswers = answerRepository.count();
        long rememberedReviews = reviewRepository.countByRememberedTrue();
        long flashcardReviews = rememberedReviews + reviewRepository.countByRememberedFalse();
        long correctAnswers = answerRepository.countByCorrectTrue();
        long totalLearningEvents = quizAnswers + flashcardReviews;
        long successfulEvents = correctAnswers + rememberedReviews;
        return new OverallProgressResponse(
                topicRepository.count(),
                attemptRepository.countByFinishedAtIsNotNull(),
                quizAnswers,
                correctAnswers,
                flashcardRepository.count(),
                flashcardReviews,
                rememberedReviews,
                masteryPercentage(successfulEvents, totalLearningEvents));
    }

    public TopicProgressResponse getByTopic(UUID topicId) {
        var topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Tema", topicId));
        long correctAnswers = answerRepository.countCorrectByQuestionTopicId(topicId);
        long incorrectAnswers = answerRepository.countIncorrectByQuestionTopicId(topicId);
        long quizAnswers = correctAnswers + incorrectAnswers;
        long rememberedReviews = reviewRepository.countByFlashcardTopicIdAndRememberedTrue(topicId);
        long forgottenReviews = reviewRepository.countByFlashcardTopicIdAndRememberedFalse(topicId);
        long flashcardReviews = rememberedReviews + forgottenReviews;
        return new TopicProgressResponse(
                topic.getId(),
                topic.getName(),
                attemptRepository.countFinishedByQuestionTopicId(topicId),
                quizAnswers,
                correctAnswers,
                flashcardRepository.countByTopicId(topicId),
                flashcardReviews,
                rememberedReviews,
                masteryPercentage(correctAnswers + rememberedReviews, quizAnswers + flashcardReviews));
    }

    public Page<FrequentErrorResponse> getFrequentErrors(UUID topicId, Pageable pageable) {
        if (topicId == null) {
            return answerRepository.findFrequentErrors(pageable).map(FrequentErrorResponse::from);
        }
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Tema", topicId);
        }
        return answerRepository.findFrequentErrorsByTopic(topicId, pageable).map(FrequentErrorResponse::from);
    }

    public Page<FlashcardResponse> getDueReviews(Pageable pageable) {
        return flashcardRepository.findAllByNextReviewAtLessThanEqual(Instant.now(), pageable)
                .map(FlashcardResponse::from);
    }

    private BigDecimal masteryPercentage(long successfulEvents, long totalEvents) {
        if (totalEvents == 0) {
            return null;
        }
        return BigDecimal.valueOf(successfulEvents)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalEvents), 2, RoundingMode.HALF_UP);
    }
}
