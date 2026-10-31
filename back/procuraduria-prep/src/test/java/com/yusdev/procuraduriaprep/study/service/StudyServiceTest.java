package com.yusdev.procuraduriaprep.study.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.yusdev.procuraduriaprep.shared.exception.DuplicateResourceException;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;
import com.yusdev.procuraduriaprep.study.dto.CreateStudyModuleRequest;
import com.yusdev.procuraduriaprep.study.dto.CreateTopicRequest;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import com.yusdev.procuraduriaprep.study.entity.StudyModule;
import com.yusdev.procuraduriaprep.study.repository.StudyModuleRepository;
import com.yusdev.procuraduriaprep.study.repository.TopicRepository;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

	@Mock
	private StudyModuleRepository moduleRepository;

	@Mock
	private TopicRepository topicRepository;

	@InjectMocks
	private StudyService studyService;

	@Test
	void createsModuleWithNormalizedNameAndActiveStatus() {
		when(moduleRepository.existsByNameIgnoreCase("Constitución")).thenReturn(false);
		when(moduleRepository.save(any(StudyModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = studyService.createModule(new CreateStudyModuleRequest(" Constitución ", " Base ", 1));

		assertEquals("Constitución", response.name());
		assertEquals("Base", response.description());
		assertEquals("ACTIVE", response.status().name());
	}

	@Test
	void rejectsDuplicateModuleName() {
		when(moduleRepository.existsByNameIgnoreCase("Constitución")).thenReturn(true);

		assertThrows(DuplicateResourceException.class,
				() -> studyService.createModule(new CreateStudyModuleRequest("Constitución", null, 0)));
	}

	@Test
	void rejectsTopicWhenModuleDoesNotExist() {
		UUID moduleId = UUID.randomUUID();
		when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> studyService.createTopic(moduleId,
				new CreateTopicRequest("Derechos", null, DifficultyLevel.BASIC)));
		verify(topicRepository, org.mockito.Mockito.never()).save(any());
	}
}
