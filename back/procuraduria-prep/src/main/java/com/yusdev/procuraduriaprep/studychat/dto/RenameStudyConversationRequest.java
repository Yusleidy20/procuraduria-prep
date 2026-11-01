package com.yusdev.procuraduriaprep.studychat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameStudyConversationRequest(@NotBlank @Size(max = 120) String title) {
}
