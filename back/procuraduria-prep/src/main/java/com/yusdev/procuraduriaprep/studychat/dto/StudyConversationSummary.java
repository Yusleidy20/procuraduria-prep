package com.yusdev.procuraduriaprep.studychat.dto;

import java.time.Instant;
import java.util.UUID;

public record StudyConversationSummary(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        long messageCount) {
}
