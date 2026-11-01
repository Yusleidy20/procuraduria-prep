package com.yusdev.procuraduriaprep.studychat.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendStudyMessageRequest(
        @NotBlank @Size(max = 2000) String content,
        @Size(max = 80) String learnerLevel,
        UUID sourceDocumentId) {
}
