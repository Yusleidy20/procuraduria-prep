package com.yusdev.procuraduriaprep.documents.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yusdev.procuraduriaprep.documents.entity.DocumentChunk;
import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findAllByDocument_IdAndDocument_StatusNot(UUID documentId, DocumentStatus status);

    List<DocumentChunk> findAllByDocument_StatusNot(DocumentStatus status);

    boolean existsByDocument_Id(UUID documentId);

    void deleteAllByDocument_Id(UUID documentId);
}
