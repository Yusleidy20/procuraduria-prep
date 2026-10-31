package com.yusdev.procuraduriaprep.flashcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;
import com.yusdev.procuraduriaprep.flashcards.entity.Flashcard;
import com.yusdev.procuraduriaprep.flashcards.repository.FlashcardRepository;
import com.yusdev.procuraduriaprep.flashcards.repository.FlashcardReviewRepository;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.StudyModule;
import com.yusdev.procuraduriaprep.study.entity.Topic;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

	@Mock
	private FlashcardRepository flashcardRepository;

	@Mock
	private FlashcardReviewRepository reviewRepository;

	@Mock
	private TopicRepository topicRepository;

	@Mock
	private StudyDocumentRepository documentRepository;

	@InjectMocks
	private FlashcardService flashcardService;

	@Test
	void recordsReviewAndSchedulesNextReview() {
		UUID cardId = UUID.randomUUID();
		StudyModule module = new StudyModule("Constitución", null, 0);
		Topic topic = new Topic("Derechos", null, module, DifficultyLevel.BASIC);
		Flashcard card = new Flashcard("¿Qué es?", "Un derecho", null, topic, null, DifficultyLevel.BASIC);
		when(flashcardRepository.findById(cardId)).thenReturn(Optional.of(card));
		when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = flashcardService.recordReview(cardId,
				new com.yusdev.procuraduriaprep.flashcards.dto.RecordReviewRequest(true));

		assertEquals(1, response.reviewCount());
		assertEquals(1, response.correctReviewCount());
		assertEquals(card.getCreatedAt().plusSeconds(24 * 60 * 60).getEpochSecond(),
				response.nextReviewAt().getEpochSecond(), 2);
		verify(reviewRepository).save(any());
	}

	@Test
	void rejectsReviewForUnknownFlashcard() {
		UUID cardId = UUID.randomUUID();
		when(flashcardRepository.findById(cardId)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class,
				() -> flashcardService.recordReview(cardId,
						new com.yusdev.procuraduriaprep.flashcards.dto.RecordReviewRequest(false)));
	}
}
