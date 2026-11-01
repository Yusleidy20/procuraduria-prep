package com.yusdev.procuraduriaprep.studychat.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.yusdev.procuraduriaprep.ai.dto.DocumentSourceReference;
import com.yusdev.procuraduriaprep.studychat.entity.StudyMessageRole;

public record StudyMessageResponse(
        UUID id,
        StudyMessageRole role,
        String content,
        String keyPoints,
        List<DocumentSourceReference> sources,
        Instant createdAt) {

    public StudyMessageResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
