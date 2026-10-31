package com.yusdev.procuraduriaprep.ai.dto;

import java.util.UUID;

public record DocumentSourceReference(UUID documentId, String documentName, Integer pageNumber, int chunkIndex) {
}
