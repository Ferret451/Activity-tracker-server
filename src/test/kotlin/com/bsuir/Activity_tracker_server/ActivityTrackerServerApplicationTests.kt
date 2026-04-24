package com.bsuir.Activity_tracker_server

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class ActivityTrackerServerApplicationTests {

	@Autowired
	lateinit var mockMvc: MockMvc

	@Test
	fun registerLoginRefreshFlowWorks() {
		val registerBody = """
			{
			  "email": "user1@example.com",
			  "username": "user1",
			  "firstName": "Test",
			  "lastName": "User",
			  "password": "password123"
			}
		""".trimIndent()

		val registerResponse = mockMvc.perform(
			post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerBody),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.accessToken").isNotEmpty)
			.andExpect(jsonPath("$.refreshToken").isNotEmpty)
			.andReturn()

		val refreshToken = Regex("\"refreshToken\"\\s*:\\s*\"([^\"]+)\"")
			.find(registerResponse.response.contentAsString)
			?.groupValues
			?.get(1)
			?: error("refreshToken was not found in register response")

		mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"user1@example.com","password":"password123"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.accessToken").isNotEmpty)
			.andExpect(jsonPath("$.refreshToken").isNotEmpty)

		mockMvc.perform(
			post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"$refreshToken"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.accessToken").isNotEmpty)
			.andExpect(jsonPath("$.refreshToken").isNotEmpty)
	}

	@Test
	fun usersEndpointRequiresAuth() {
		mockMvc.perform(get("/api/users"))
			.andExpect(status().isUnauthorized)
	}

}
