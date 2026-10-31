package com.yusdev.procuraduriaprep.quizzes.repository;

import java.util.UUID;

public interface FrequentQuizError {

	UUID getQuestionId();

	String getQuestionStatement();

	UUID getTopicId();

	String getTopicName();

	long getErrorCount();
}
