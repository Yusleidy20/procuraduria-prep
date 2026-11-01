package com.yusdev.procuraduriaprep.studychat.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.studychat.entity.StudyConversation;
import com.yusdev.procuraduriaprep.studychat.entity.StudyMessage;
import com.yusdev.procuraduriaprep.studychat.entity.StudyMessageRole;
import com.yusdev.procuraduriaprep.users.entity.AppUser;
import com.yusdev.procuraduriaprep.users.repository.AppUserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudyConversationRepositoryTest {

    @Autowired private AppUserRepository userRepository;
    @Autowired private StudyConversationRepository conversationRepository;
    @Autowired private StudyMessageRepository messageRepository;

    @Test
    void persistsMessagesScopesConversationsToOwnerAndCascadesDeletion() {
        AppUser owner = userRepository.saveAndFlush(new AppUser("Owner", uniqueEmail(), "hash"));
        StudyConversation conversation = conversationRepository.saveAndFlush(
                new StudyConversation(owner, "Organización del Estado"));
        StudyMessage message = new StudyMessage(conversation, StudyMessageRole.ASSISTANT,
                "Explicación basada en fuentes [S1].",
                "{\"keyPoints\":\"Funciones diferenciadas\",\"sources\":[]}");
        conversation.addMessage(message);
        message = messageRepository.saveAndFlush(message);
        UUID conversationId = conversation.getId();

        assertTrue(conversationRepository.findByIdAndUser_Id(conversationId, owner.getId()).isPresent());
        assertTrue(conversationRepository.findByIdAndUser_Id(conversationId, UUID.randomUUID()).isEmpty());
        assertEquals(1, conversationRepository.findAllByUser_IdOrderByUpdatedAtDesc(owner.getId()).size());
        assertEquals(message.getId(), messageRepository.findAllByConversation_IdOrderByCreatedAtAscIdAsc(conversationId)
                .get(0).getId());
        assertEquals(1, messageRepository.countByConversation_Id(conversationId));

        conversationRepository.deleteById(conversationId);
        conversationRepository.flush();
        assertEquals(0, messageRepository.countByConversation_Id(conversationId));
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
