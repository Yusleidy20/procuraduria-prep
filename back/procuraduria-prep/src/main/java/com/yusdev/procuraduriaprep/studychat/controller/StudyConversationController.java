package com.yusdev.procuraduriaprep.studychat.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.yusdev.procuraduriaprep.studychat.dto.CreateStudyConversationRequest;
import com.yusdev.procuraduriaprep.studychat.dto.RenameStudyConversationRequest;
import com.yusdev.procuraduriaprep.studychat.dto.SendStudyMessageRequest;
import com.yusdev.procuraduriaprep.studychat.dto.StudyChatResponse;
import com.yusdev.procuraduriaprep.studychat.dto.StudyConversationDetail;
import com.yusdev.procuraduriaprep.studychat.dto.StudyConversationSummary;
import com.yusdev.procuraduriaprep.studychat.service.StudyConversationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/study/conversations")
public class StudyConversationController {

    private final StudyConversationService conversationService;

    public StudyConversationController(StudyConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<StudyConversationSummary> list(Authentication authentication) {
        return conversationService.list(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<StudyConversationSummary> create(
            Authentication authentication,
            @Valid @RequestBody CreateStudyConversationRequest request,
            UriComponentsBuilder uriBuilder) {
        StudyConversationSummary conversation = conversationService.create(authentication.getName(), request);
        URI location = uriBuilder.path("/api/v1/study/conversations/{id}")
                .buildAndExpand(conversation.id()).toUri();
        return ResponseEntity.created(location).body(conversation);
    }

    @GetMapping("/{conversationId}")
    public StudyConversationDetail get(Authentication authentication, @PathVariable UUID conversationId) {
        return conversationService.get(authentication.getName(), conversationId);
    }

    @PatchMapping("/{conversationId}")
    public StudyConversationSummary rename(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody RenameStudyConversationRequest request) {
        return conversationService.rename(authentication.getName(), conversationId, request);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID conversationId) {
        conversationService.delete(authentication.getName(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<StudyChatResponse> send(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendStudyMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.send(authentication.getName(), conversationId, request));
    }
}
