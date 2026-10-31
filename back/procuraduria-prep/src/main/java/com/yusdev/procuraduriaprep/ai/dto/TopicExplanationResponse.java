package com.yusdev.procuraduriaprep.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TopicExplanationResponse(
		@NotBlank @Size(max = 12000) String explanation,
		@Size(max = 5000) String keyPoints,
		List<DocumentSourceReference> sources) {

	public TopicExplanationResponse(String explanation, String keyPoints) {
		this(explanation, keyPoints, List.of());
	}

	public TopicExplanationResponse {
		sources = sources == null ? List.of() : List.copyOf(sources);
	}
}
