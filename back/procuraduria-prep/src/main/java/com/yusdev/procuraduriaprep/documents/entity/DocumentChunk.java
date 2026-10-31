package com.yusdev.procuraduriaprep.documents.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "document_chunks", uniqueConstraints = @UniqueConstraint(
        name = "uk_document_chunk_index", columnNames = {"document_id", "chunk_index"}))
public class DocumentChunk {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private StudyDocument document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "term_vector", nullable = false, columnDefinition = "text")
    private String termVector;

    protected DocumentChunk() {
    }

    public DocumentChunk(StudyDocument document, int chunkIndex, Integer pageNumber, String text, String termVector) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.text = text;
        this.termVector = termVector;
    }

    public UUID getId() { return id; }
    public StudyDocument getDocument() { return document; }
    public int getChunkIndex() { return chunkIndex; }
    public Integer getPageNumber() { return pageNumber; }
    public String getText() { return text; }
    public String getTermVector() { return termVector; }
}
