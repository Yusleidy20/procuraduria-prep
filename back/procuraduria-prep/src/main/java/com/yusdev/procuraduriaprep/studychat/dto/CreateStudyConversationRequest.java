package com.yusdev.procuraduriaprep.studychat.dto;

import jakarta.validation.constraints.Size;

public record CreateStudyConversationRequest(@Size(max = 120) String title) {
}
