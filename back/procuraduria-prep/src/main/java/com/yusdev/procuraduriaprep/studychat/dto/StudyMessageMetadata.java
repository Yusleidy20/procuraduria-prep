package com.yusdev.procuraduriaprep.studychat.dto;

import java.util.List;

import com.yusdev.procuraduriaprep.ai.dto.DocumentSourceReference;

public record StudyMessageMetadata(String keyPoints, List<DocumentSourceReference> sources) {

    public StudyMessageMetadata {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public static StudyMessageMetadata empty() {
        return new StudyMessageMetadata(null, List.of());
    }
}
