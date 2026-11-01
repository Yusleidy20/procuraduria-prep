package com.yusdev.procuraduriaprep.studychat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yusdev.procuraduriaprep.studychat.entity.StudyConversation;

public interface StudyConversationRepository extends JpaRepository<StudyConversation, UUID> {

    List<StudyConversation> findAllByUser_IdOrderByUpdatedAtDesc(UUID userId);

    Optional<StudyConversation> findByIdAndUser_Id(UUID id, UUID userId);
}
