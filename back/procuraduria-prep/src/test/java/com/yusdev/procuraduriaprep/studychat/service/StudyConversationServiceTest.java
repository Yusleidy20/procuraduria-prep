package com.yusdev.procuraduriaprep.studychat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import com.yusdev.procuraduriaprep.ai.dto.DocumentSourceReference;
import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;
import com.yusdev.procuraduriaprep.ai.service.AiProviderException;
import com.yusdev.procuraduriaprep.ai.service.AiTutorService;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;
import com.yusdev.procuraduriaprep.studychat.dto.CreateStudyConversationRequest;
import com.yusdev.procuraduriaprep.studychat.dto.RenameStudyConversationRequest;
import com.yusdev.procuraduriaprep.studychat.dto.SendStudyMessageRequest;
import com.yusdev.procuraduriaprep.studychat.entity.StudyConversation;
import com.yusdev.procuraduriaprep.studychat.entity.StudyMessage;
import com.yusdev.procuraduriaprep.studychat.entity.StudyMessageRole;
import com.yusdev.procuraduriaprep.studychat.repository.StudyConversationRepository;
import com.yusdev.procuraduriaprep.studychat.repository.StudyMessageRepository;
import com.yusdev.procuraduriaprep.users.entity.AppUser;
import com.yusdev.procuraduriaprep.users.repository.AppUserRepository;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class StudyConversationServiceTest {

    private static final String EMAIL = "student@example.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    @Mock private StudyConversationRepository conversationRepository;
    @Mock private StudyMessageRepository messageRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private AiTutorService aiTutorService;
    @Mock private AppUser user;
    @Mock private StudyConversation conversation;

    private StudyConversationService service;

    @BeforeEach
    void setUp() {
        service = new StudyConversationService(
                conversationRepository, messageRepository, userRepository, aiTutorService,
                JsonMapper.builder().build());
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(USER_ID);
    }

    @Test
    void createsAndListsConversationsForTheAuthenticatedUser() {
        when(conversationRepository.save(any(StudyConversation.class))).thenAnswer(invocation -> {
            StudyConversation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", CONVERSATION_ID);
            ReflectionTestUtils.setField(saved, "createdAt", Instant.parse("2026-10-31T12:00:00Z"));
            ReflectionTestUtils.setField(saved, "updatedAt", Instant.parse("2026-10-31T12:00:00Z"));
            when(messageRepository.countByConversation_Id(CONVERSATION_ID)).thenReturn(0L);
            return saved;
        });

        var created = service.create(EMAIL, new CreateStudyConversationRequest(null));

        assertEquals(CONVERSATION_ID, created.id());
        assertEquals(StudyConversation.DEFAULT_TITLE, created.title());
        assertEquals(List.of(), service.list(EMAIL));
        verify(conversationRepository).findAllByUser_IdOrderByUpdatedAtDesc(USER_ID);
    }

    @Test
    void sendsAndPersistsUserAndAssistantMessagesWithRealRetrievedSources() {
        UUID sourceId = UUID.randomUUID();
        DocumentSourceReference source = new DocumentSourceReference(sourceId, "Constitución.pdf", 4, 2);
        when(conversationRepository.findByIdAndUser_Id(CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.of(conversation));
        when(conversation.getId()).thenReturn(CONVERSATION_ID);
        when(conversation.getTitle()).thenReturn(StudyConversation.DEFAULT_TITLE);
        when(conversation.getCreatedAt()).thenReturn(Instant.now());
        when(conversation.getUpdatedAt()).thenReturn(Instant.now());
        when(messageRepository.findTop10ByConversation_IdOrderByCreatedAtDescIdDesc(CONVERSATION_ID))
                .thenReturn(List.of());
        when(aiTutorService.explainTopic(any())).thenReturn(
                new TopicExplanationResponse("Las ramas del poder cumplen funciones distintas [S1].",
                        "Funciones diferenciadas", List.of(source)));
        when(messageRepository.save(any(StudyMessage.class))).thenAnswer(invocation -> {
            StudyMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(message, "createdAt", Instant.now());
            return message;
        });
        when(messageRepository.countByConversation_Id(CONVERSATION_ID)).thenReturn(2L);

        var response = service.send(EMAIL, CONVERSATION_ID, new SendStudyMessageRequest(
                "Explícame la diferencia entre las ramas", "Inicial", null));

        assertEquals(StudyMessageRole.USER, response.userMessage().role());
        assertEquals(StudyMessageRole.ASSISTANT, response.assistantMessage().role());
        assertEquals(List.of(source), response.assistantMessage().sources());
        assertEquals("Funciones diferenciadas", response.assistantMessage().keyPoints());
        ArgumentCaptor<ExplainTopicRequest> requestCaptor = ArgumentCaptor.forClass(ExplainTopicRequest.class);
        verify(aiTutorService).explainTopic(requestCaptor.capture());
        assertEquals(null, requestCaptor.getValue().sourceDocumentId());
        assertEquals("Explícame la diferencia entre las ramas", requestCaptor.getValue().topicName());
        verify(messageRepository, org.mockito.Mockito.times(2)).save(any(StudyMessage.class));
        verify(conversationRepository).save(conversation);
    }

    @Test
    void suppliesRecentUserAndAssistantMessagesAsContextForFollowUps() {
        StudyMessage previousUser = org.mockito.Mockito.mock(StudyMessage.class);
        StudyMessage previousAssistant = org.mockito.Mockito.mock(StudyMessage.class);
        when(previousUser.getRole()).thenReturn(StudyMessageRole.USER);
        when(previousUser.getContent()).thenReturn("Explícame la organización del Estado");
        when(previousAssistant.getRole()).thenReturn(StudyMessageRole.ASSISTANT);
        when(previousAssistant.getContent()).thenReturn("El Estado se organiza en ramas del poder público.");
        when(conversationRepository.findByIdAndUser_Id(CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.of(conversation));
        when(conversation.getId()).thenReturn(CONVERSATION_ID);
        when(conversation.getTitle()).thenReturn("Organización del Estado");
        when(conversation.getCreatedAt()).thenReturn(Instant.now());
        when(conversation.getUpdatedAt()).thenReturn(Instant.now());
        when(messageRepository.findTop10ByConversation_IdOrderByCreatedAtDescIdDesc(CONVERSATION_ID))
                .thenReturn(List.of(previousAssistant, previousUser));
        when(aiTutorService.explainTopic(any())).thenReturn(
                new TopicExplanationResponse("Un ejemplo de esas ramas es...", null, List.of()));
        when(messageRepository.save(any(StudyMessage.class))).thenAnswer(invocation -> {
            StudyMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(message, "createdAt", Instant.now());
            return message;
        });
        when(messageRepository.countByConversation_Id(CONVERSATION_ID)).thenReturn(4L);

        service.send(EMAIL, CONVERSATION_ID, new SendStudyMessageRequest("Dame un ejemplo", "Inicial", null));

        ArgumentCaptor<ExplainTopicRequest> requestCaptor = ArgumentCaptor.forClass(ExplainTopicRequest.class);
        verify(aiTutorService).explainTopic(requestCaptor.capture());
        assertEquals(List.of("Explícame la organización del Estado"), requestCaptor.getValue().previousQuestions());
        assertEquals(2, requestCaptor.getValue().conversationHistory().size());
        assertEquals("El Estado se organiza en ramas del poder público.",
                requestCaptor.getValue().conversationHistory().get(1).content());
    }

    @Test
    void hidesConversationsOwnedByAnotherUserAndDoesNotCallTheTutor() {
        when(conversationRepository.findByIdAndUser_Id(CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(EMAIL, CONVERSATION_ID));
        assertThrows(ResourceNotFoundException.class,
                () -> service.send(EMAIL, CONVERSATION_ID, new SendStudyMessageRequest("Dame un ejemplo", null, null)));
        verify(aiTutorService, never()).explainTopic(any());
    }

    @Test
    void propagatesProviderFailureWithoutPersistingAnIncompleteTurn() {
        when(conversationRepository.findByIdAndUser_Id(CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findTop10ByConversation_IdOrderByCreatedAtDescIdDesc(CONVERSATION_ID))
                .thenReturn(List.of());
        when(aiTutorService.explainTopic(any()))
                .thenThrow(new AiProviderException("AI provider unavailable", "groq", true, 503));

        assertThrows(AiProviderException.class, () -> service.send(
                EMAIL, CONVERSATION_ID, new SendStudyMessageRequest("Explícame el tema", "Inicial", null)));
        verify(messageRepository, never()).save(any(StudyMessage.class));
    }

    @Test
    void renamesAndDeletesOnlyTheOwnedConversation() {
        when(conversationRepository.findByIdAndUser_Id(CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.of(conversation));
        when(conversation.getId()).thenReturn(CONVERSATION_ID);
        when(conversation.getTitle()).thenReturn("Nuevo título");
        when(conversation.getCreatedAt()).thenReturn(Instant.now());
        when(conversation.getUpdatedAt()).thenReturn(Instant.now());
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(messageRepository.countByConversation_Id(CONVERSATION_ID)).thenReturn(0L);

        var renamed = service.rename(EMAIL, CONVERSATION_ID, new RenameStudyConversationRequest("Nuevo título"));
        service.delete(EMAIL, CONVERSATION_ID);

        assertEquals("Nuevo título", renamed.title());
        verify(conversation).rename("Nuevo título");
        verify(conversationRepository).delete(conversation);
    }
}
