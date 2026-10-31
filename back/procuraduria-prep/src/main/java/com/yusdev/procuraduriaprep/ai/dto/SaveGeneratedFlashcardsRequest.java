package com.yusdev.procuraduriaprep.ai.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveGeneratedFlashcardsRequest(
        @NotNull UUID topicId,
        UUID sourceDocumentId,
        @NotEmpty @Size(max = 20) List<@Valid GeneratedFlashcard> flashcards) {
}
