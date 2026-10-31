package com.yusdev.procuraduriaprep.ai.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class GeminiClientConfig {

	@Bean(name = "geminiRestClientBuilder")
	RestClient.Builder restClientBuilder(
				@Value("${app.ai.gemini.connect-timeout:3s}") Duration connectTimeout,
				@Value("${app.ai.gemini.response-timeout:6s}") Duration responseTimeout) {
		var httpClient = java.net.http.HttpClient.newBuilder()
				.connectTimeout(connectTimeout)
				.build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(responseTimeout);
		return RestClient.builder().requestFactory(requestFactory);
	}

	@Bean(name = "groqRestClientBuilder")
	RestClient.Builder groqRestClientBuilder(
			@Value("${app.ai.groq.connect-timeout:3s}") Duration connectTimeout,
			@Value("${app.ai.groq.response-timeout:30s}") Duration responseTimeout) {
		var httpClient = java.net.http.HttpClient.newBuilder()
				.connectTimeout(connectTimeout)
				.build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(responseTimeout);
		return RestClient.builder().requestFactory(requestFactory);
	}
}
