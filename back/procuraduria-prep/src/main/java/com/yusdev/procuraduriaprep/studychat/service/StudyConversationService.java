package com.yusdev.procuraduriaprep.studychat.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.dto.StudyConversationContextMessage;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;
import com.yusdev.procuraduriaprep.ai.service.AiTutorService;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;
import com.yusdev.procuraduriaprep.studychat.dto.CreateStudyConversationRequest;
import com.yusdev.procuraduriaprep.studychat.dto.RenameStudyConversationRequest;
import com.yusdev.procuraduriaprep.studychat.dto.SendStudyMessageRequest;
import com.yusdev.procuraduriaprep.studychat.dto.StudyChatResponse;
import com.yusdev.procuraduriaprep.studychat.dto.StudyConversationDetail;
import com.yusdev.procuraduriaprep.studychat.dto.StudyConversationSummary;
import com.yusdev.procuraduriaprep.studychat.dto.StudyMessageMetadata;
import com.yusdev.procuraduriaprep.studychat.dto.StudyMessageResponse;
import com.yusdev.procuraduriaprep.studychat.entity.StudyConversation;
import com.yusdev.procuraduriaprep.studychat.entity.StudyMessage;
import com.yusdev.procuraduriaprep.studychat.entity.StudyMessageRole;
import com.yusdev.procuraduriaprep.studychat.repository.StudyConversationRepository;
import com.yusdev.procuraduriaprep.studychat.repository.StudyMessageRepository;
import com.yusdev.procuraduriaprep.users.entity.AppUser;
import com.yusdev.procuraduriaprep.users.repository.AppUserRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class StudyConversationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudyConversationService.class);
    private static final int MAX_HISTORY_MESSAGE_CHARS = 1500;
    private static final int MAX_RECOVERY_QUESTIONS = 5;
    private static final int MAX_RECOVERY_QUESTION_CHARS = 500;
    private static final int MAX_TITLE_LENGTH = 120;

    private final StudyConversationRepository conversationRepository;
    private final StudyMessageRepository messageRepository;
    private final AppUserRepository userRepository;
    private final AiTutorService aiTutorService;
    private final ObjectMapper objectMapper;

    public StudyConversationService(
            StudyConversationRepository conversationRepository,
            StudyMessageRepository messageRepository,
            AppUserRepository userRepository,
            AiTutorService aiTutorService,
            ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiTutorService = aiTutorService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<StudyConversationSummary> list(String authenticatedEmail) {
        AppUser user = requireUser(authenticatedEmail);
        return conversationRepository.findAllByUser_IdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(this::summary)
                .toList();
    }

    @Transactional
    public StudyConversationSummary create(String authenticatedEmail, CreateStudyConversationRequest request) {
        AppUser user = requireUser(authenticatedEmail);
        StudyConversation conversation = new StudyConversation(user, request.title());
        return summary(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public StudyConversationDetail get(String authenticatedEmail, UUID conversationId) {
        StudyConversation conversation = requireOwnedConversation(authenticatedEmail, conversationId);
        List<StudyMessageResponse> messages = messageRepository
            .findAllByConversation_IdOrderByCreatedAtAscIdAsc(conversationId).stream()
                .map(this::messageResponse)
                .toList();
        return new StudyConversationDetail(summary(conversation), messages);
    }

    @Transactional
    public StudyConversationSummary rename(
            String authenticatedEmail, UUID conversationId, RenameStudyConversationRequest request) {
        StudyConversation conversation = requireOwnedConversation(authenticatedEmail, conversationId);
        conversation.rename(request.title());
        return summary(conversationRepository.save(conversation));
    }

    @Transactional
    public void delete(String authenticatedEmail, UUID conversationId) {
        StudyConversation conversation = requireOwnedConversation(authenticatedEmail, conversationId);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public StudyChatResponse send(
            String authenticatedEmail, UUID conversationId, SendStudyMessageRequest request) {
        StudyConversation conversation = requireOwnedConversation(authenticatedEmail, conversationId);
        List<StudyMessage> recentMessages = recentMessages(conversationId);
        List<String> recoveryQuestions = recentMessages.stream()
                .filter(message -> message.getRole() == StudyMessageRole.USER)
                .map(message -> truncate(message.getContent(), MAX_RECOVERY_QUESTION_CHARS))
                .skip(Math.max(0, recentMessages.stream()
                        .filter(message -> message.getRole() == StudyMessageRole.USER).count() - MAX_RECOVERY_QUESTIONS))
                .toList();
        List<StudyConversationContextMessage> history = recentMessages.stream()
                .map(message -> new StudyConversationContextMessage(
                        message.getRole().name(), truncate(message.getContent(), MAX_HISTORY_MESSAGE_CHARS)))
                .toList();

        TopicExplanationResponse explanation = aiTutorService.explainTopic(new ExplainTopicRequest(
                request.content().strip(), null, request.learnerLevel(), request.sourceDocumentId(),
                recoveryQuestions, history));

        StudyMessage userMessage = new StudyMessage(
                conversation, StudyMessageRole.USER, request.content().strip(), "{}");
        StudyMessage assistantMessage = new StudyMessage(
                conversation, StudyMessageRole.ASSISTANT, explanation.explanation(),
                serializeMetadata(new StudyMessageMetadata(explanation.keyPoints(), explanation.sources())));
        conversation.addMessage(userMessage);
        conversation.addMessage(assistantMessage);
        messageRepository.save(userMessage);
        messageRepository.save(assistantMessage);
        if (StudyConversation.DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.setInitialTitle(truncate(request.content().strip(), MAX_TITLE_LENGTH));
        } else {
            conversation.touch();
        }
        conversationRepository.save(conversation);

        return new StudyChatResponse(summary(conversation), messageResponse(userMessage), messageResponse(assistantMessage));
    }

    private AppUser requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado", email));
    }

    private StudyConversation requireOwnedConversation(String email, UUID conversationId) {
        AppUser user = requireUser(email);
        return conversationRepository.findByIdAndUser_Id(conversationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversación", conversationId));
    }

    private List<StudyMessage> recentMessages(UUID conversationId) {
        List<StudyMessage> messages = new ArrayList<>(
                messageRepository.findTop10ByConversation_IdOrderByCreatedAtDescIdDesc(conversationId));
        Collections.reverse(messages);
        return messages;
    }

    private StudyConversationSummary summary(StudyConversation conversation) {
        return new StudyConversationSummary(
                conversation.getId(), conversation.getTitle(), conversation.getCreatedAt(),
                conversation.getUpdatedAt(), messageRepository.countByConversation_Id(conversation.getId()));
    }

    private StudyMessageResponse messageResponse(StudyMessage message) {
        StudyMessageMetadata metadata;
        try {
            metadata = objectMapper.readValue(message.getMetadata(), StudyMessageMetadata.class);
        } catch (JacksonException exception) {
            LOGGER.warn("Stored chat metadata for message {} is invalid; omitting optional citations", message.getId());
            metadata = StudyMessageMetadata.empty();
        }
        return new StudyMessageResponse(message.getId(), message.getRole(), message.getContent(),
                metadata.keyPoints(), metadata.sources(), message.getCreatedAt());
    }

    private String serializeMetadata(StudyMessageMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            throw new IllegalStateException("No fue posible guardar los metadatos de fuentes de la respuesta", exception);
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength).stripTrailing();
    }
}
