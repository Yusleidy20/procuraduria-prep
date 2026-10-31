package com.yusdev.procuraduriaprep.ai.service;

import java.util.Map;

@FunctionalInterface
public interface AiProvider {

    String generate(String prompt, Map<String, Object> responseSchema);
}
