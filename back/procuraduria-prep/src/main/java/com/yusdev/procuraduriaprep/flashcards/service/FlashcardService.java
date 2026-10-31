package com.yusdev.procuraduriaprep.flashcards.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;
import com.yusdev.procuraduriaprep.flashcards.dto.CreateFlashcardRequest;
import com.yusdev.procuraduriaprep.flashcards.dto.FlashcardResponse;
import com.yusdev.procuraduriaprep.flashcards.dto.RecordReviewRequest;
import com.yusdev.procuraduriaprep.flashcards.entity.Flashcard;
import com.yusdev.procuraduriaprep.flashcards.entity.FlashcardReview;
import com.yusdev.procuraduriaprep.flashcards.repository.FlashcardRepository;
import com.yusdev.procuraduriaprep.flashcards.repository.FlashcardReviewRepository;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;
import com.yusdev.procuraduriaprep.study.entity.Topic;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@Service
@Transactional(readOnly = true)
public class FlashcardService {

	private final FlashcardRepository flashcardRepository;
	private final FlashcardReviewRepository reviewRepository;
	private final TopicRepository topicRepository;
	private final StudyDocumentRepository documentRepository;

	public FlashcardService(FlashcardRepository flashcardRepository,
			FlashcardReviewRepository reviewRepository,
			TopicRepository topicRepository,
			StudyDocumentRepository documentRepository) {
		this.flashcardRepository = flashcardRepository;
		this.reviewRepository = reviewRepository;
		this.topicRepository = topicRepository;
		this.documentRepository = documentRepository;
	}

	@Transactional
	public FlashcardResponse create(UUID topicId, CreateFlashcardRequest request) {
		Topic topic = topicRepository.findById(topicId)
				.orElseThrow(() -> new ResourceNotFoundException("Tema", topicId));
		StudyDocument source = request.sourceDocumentId() == null ? null
				: documentRepository.findById(request.sourceDocumentId())
						.orElseThrow(() -> new ResourceNotFoundException("Documento", request.sourceDocumentId()));
		if (source != null && !source.isActive()) {
			throw new ResourceNotFoundException("Documento activo", source.getId());
		}

		Flashcard flashcard = flashcardRepository.save(new Flashcard(request.question().trim(), request.answer().trim(),
				normalize(request.explanation()), topic, source, request.difficulty()));
		return FlashcardResponse.from(flashcard);
	}

	public Page<FlashcardResponse> getByTopic(UUID topicId, Pageable pageable) {
		if (!topicRepository.existsById(topicId)) {
			throw new ResourceNotFoundException("Tema", topicId);
		}
		return flashcardRepository.findAllByTopicId(topicId, pageable).map(FlashcardResponse::from);
	}

	public Page<FlashcardResponse> getDue(Pageable pageable) {
		return flashcardRepository.findAllByNextReviewAtLessThanEqual(Instant.now(), pageable)
				.map(FlashcardResponse::from);
	}

	public FlashcardResponse getById(UUID flashcardId) {
		Flashcard flashcard = flashcardRepository.findById(flashcardId)
				.orElseThrow(() -> new ResourceNotFoundException("Tarjeta", flashcardId));
		return FlashcardResponse.from(flashcard);
	}

	@Transactional
	public FlashcardResponse recordReview(UUID flashcardId, RecordReviewRequest request) {
		Flashcard flashcard = flashcardRepository.findById(flashcardId)
				.orElseThrow(() -> new ResourceNotFoundException("Tarjeta", flashcardId));
		Instant reviewedAt = Instant.now();
		flashcard.recordReview(request.remembered(), reviewedAt);
		reviewRepository.save(new FlashcardReview(flashcard, request.remembered(), reviewedAt));
		return FlashcardResponse.from(flashcard);
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
