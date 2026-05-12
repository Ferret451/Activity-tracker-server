package com.bsuir.Activity_tracker_server.support

import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.UserRole
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    protected val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

    @MockitoBean
    protected lateinit var foodDataClient: FoodDataClient

    protected fun MockHttpServletRequestBuilder.withBearer(accessToken: String): MockHttpServletRequestBuilder =
        header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")

    protected fun registerUser(prefix: String = "user"): RegisteredUser {
        val unique = UUID.randomUUID().toString().substring(0, 8)
        val email = "$prefix-$unique@example.com"
        val username = "${prefix.replace('-', '_')}_$unique"

        val body = """
            {
              "email": "$email",
              "username": "$username",
              "firstName": "Test",
              "lastName": "User",
              "password": "password123"
            }
        """.trimIndent()

        val responseJson = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.user.id").isNotEmpty)
            .andReturn()
            .response
            .contentAsString

        val json = objectMapper.readTree(responseJson)
        val accessToken = json.requiredText("accessToken")
        val refreshToken = json.requiredText("refreshToken")
        val userId = UUID.fromString(json.path("user").path("id").asText())
        assertNotNull(accessToken)
        assertNotNull(refreshToken)

        return RegisteredUser(userId = userId, email = email, accessToken = accessToken, refreshToken = refreshToken)
    }

    protected fun promoteToAdmin(email: String) {
        val user = userRepository.findByEmailIgnoreCase(email) ?: error("User not found for email $email")
        user.role = UserRole.ADMIN
        userRepository.save(user)
    }

    protected fun refreshAccessToken(refreshToken: String): String {
        val responseJson =
            mockMvc.perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"$refreshToken"}"""),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").isNotEmpty)
                .andReturn()
                .response
                .contentAsString
        return objectMapper.readTree(responseJson).requiredText("accessToken")
    }

    protected fun registerAdminUser(prefix: String = "admin"): RegisteredUser {
        val user = registerUser(prefix)
        promoteToAdmin(user.email)
        val newAccess = refreshAccessToken(user.refreshToken)
        return user.copy(accessToken = newAccess)
    }

    protected data class RegisteredUser(
        val userId: UUID,
        val email: String,
        val accessToken: String,
        val refreshToken: String,
    )
}

private fun JsonNode.requiredText(field: String): String =
    path(field).takeIf { it.isTextual }?.asText() ?: error("Field '$field' missing or not text")

