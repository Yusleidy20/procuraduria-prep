package com.yusdev.procuraduriaprep.studychat.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yusdev.procuraduriaprep.studychat.entity.StudyMessage;

public interface StudyMessageRepository extends JpaRepository<StudyMessage, UUID> {

    List<StudyMessage> findAllByConversation_IdOrderByCreatedAtAscIdAsc(UUID conversationId);

    List<StudyMessage> findTop10ByConversation_IdOrderByCreatedAtDescIdDesc(UUID conversationId);

    long countByConversation_Id(UUID conversationId);
}
