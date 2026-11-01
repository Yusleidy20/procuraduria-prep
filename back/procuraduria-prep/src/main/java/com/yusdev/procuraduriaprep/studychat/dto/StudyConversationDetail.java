package com.yusdev.procuraduriaprep.studychat.dto;

import java.util.List;

public record StudyConversationDetail(
        StudyConversationSummary conversation,
        List<StudyMessageResponse> messages) {

    public StudyConversationDetail {
        messages = List.copyOf(messages);
    }
}
