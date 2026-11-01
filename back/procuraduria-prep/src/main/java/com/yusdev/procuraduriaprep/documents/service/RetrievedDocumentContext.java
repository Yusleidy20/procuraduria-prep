package com.yusdev.procuraduriaprep.documents.service;

import java.util.List;

import com.yusdev.procuraduriaprep.ai.dto.DocumentSourceReference;

public record RetrievedDocumentContext(String text, List<DocumentSourceReference> sources, boolean libraryEmpty) {

    public RetrievedDocumentContext(String text, List<DocumentSourceReference> sources) {
        this(text, sources, false);
    }

    public RetrievedDocumentContext {
        sources = List.copyOf(sources);
    }

    public boolean isEmpty() {
        return sources.isEmpty();
    }
}
