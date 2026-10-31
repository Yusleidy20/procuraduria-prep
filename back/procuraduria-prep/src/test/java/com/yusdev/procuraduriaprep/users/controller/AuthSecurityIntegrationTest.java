package com.yusdev.procuraduriaprep.users.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void requiresAuthenticationForStudyApi() throws Exception {
		mockMvc.perform(get("/api/v1/study/modules"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registersOnlyInitialUserAndAuthenticatesWithEncodedPassword() throws Exception {
		String request = """
				{
				  "name": "Estudiante",
				  "email": "Estudiante@Example.com",
				  "password": "contrasena-local-segura"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("estudiante@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		mockMvc.perform(get("/api/v1/auth/me")
						.with(httpBasic("estudiante@example.com", "contrasena-local-segura")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Estudiante"));

		mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void protectsRegistrationFromCrossSiteRequestsWithoutCsrfToken() throws Exception {
		String request = """
				{
				  "name": "Estudiante",
				  "email": "estudiante@example.com",
				  "password": "contrasena-local-segura"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isForbidden());
	}
}
