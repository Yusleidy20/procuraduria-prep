package com.yusdev.procuraduriaprep.documents.dto;

import java.util.UUID;

public record DocumentTextResponse(UUID documentId, String originalName, String text) {
}
