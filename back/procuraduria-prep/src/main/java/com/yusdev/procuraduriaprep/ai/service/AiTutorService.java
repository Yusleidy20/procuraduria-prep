package com.yusdev.procuraduriaprep.ai.service;

import java.util.List;

import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.dto.GenerateStudyContentRequest;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedFlashcard;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedQuizQuestion;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;

public interface AiTutorService {

	TopicExplanationResponse explainTopic(ExplainTopicRequest request);

	List<GeneratedFlashcard> generateFlashcards(GenerateStudyContentRequest request);

	List<GeneratedQuizQuestion> generateQuestions(GenerateStudyContentRequest request);
}
