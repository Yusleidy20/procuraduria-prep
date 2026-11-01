package com.yusdev.procuraduriaprep.documents.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;

public interface StudyDocumentRepository extends JpaRepository<StudyDocument, UUID> {

	Page<StudyDocument> findAllByStatusNot(DocumentStatus status, Pageable pageable);

	long countByStatusNot(DocumentStatus status);

	@Query("""
			select document from StudyDocument document
			where document.status = :completed
				and document.extractedText is not null
				and not exists (
					select chunk.id from DocumentChunk chunk
					where chunk.document = document
				)
			""")
	List<StudyDocument> findCompletedDocumentsWithoutChunks(DocumentStatus completed);
}
