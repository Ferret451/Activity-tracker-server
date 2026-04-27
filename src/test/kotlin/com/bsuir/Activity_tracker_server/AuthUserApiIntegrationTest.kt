package com.bsuir.Activity_tracker_server

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class AuthUserApiIntegrationTest : ApiFunctionalCoverageIntegrationTest() {

    @Test
    fun `auth and user endpoints work end-to-end`() {
        val registerResponse = register("auth-user")
        val accessToken = registerResponse.accessToken
        val refreshToken = registerResponse.refreshToken

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "${registerResponse.email}",
                      "password": "password123"
                    }
                    """.trimIndent(),
                ),
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

        val createdUserId = extractId(
            mockMvc.perform(
                post("/api/users")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "managed-user-${UUID.randomUUID()}@example.com",
                          "username": "managed_user_${UUID.randomUUID().toString().substring(0, 8)}",
                          "firstName": "Managed",
                          "lastName": "User",
                          "password": "password123"
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").isNotEmpty)
                .andReturn()
                .response
                .contentAsString,
        )

        mockMvc.perform(get("/api/users").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/users/$createdUserId").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(createdUserId))

        mockMvc.perform(
            put("/api/users/$createdUserId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "managed-updated-${UUID.randomUUID()}@example.com",
                      "username": "managed_updated_${UUID.randomUUID().toString().substring(0, 8)}",
                      "firstName": "Updated",
                      "lastName": "User"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Updated"))

        mockMvc.perform(delete("/api/users/$createdUserId").withBearer(accessToken))
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refreshToken"}"""),
        )
            .andExpect(status().isNoContent)
    }
}

