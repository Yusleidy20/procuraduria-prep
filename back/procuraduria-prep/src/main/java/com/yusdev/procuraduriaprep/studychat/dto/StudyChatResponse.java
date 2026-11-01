package com.yusdev.procuraduriaprep.studychat.dto;

public record StudyChatResponse(
        StudyConversationSummary conversation,
        StudyMessageResponse userMessage,
        StudyMessageResponse assistantMessage) {
}
