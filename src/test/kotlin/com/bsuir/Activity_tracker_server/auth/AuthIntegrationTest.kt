package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `register then login then refresh works`() {
        val registered = registerUser("auth")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "${registered.email}",
                      "password": "password123"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.id").isNotEmpty)

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"${registered.refreshToken}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.id").isNotEmpty)
    }

    @Test
    fun `logout invalidates refresh token`() {
        val registered = registerUser("logout")

        mockMvc.perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"${registered.refreshToken}"}"""),
        )
            .andExpect(status().isNoContent)

        // Refreshing with the same token should now fail
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"${registered.refreshToken}"}"""),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `validation rejects short password`() {
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "bad@example.com",
                      "username": "bad",
                      "firstName": "Bad",
                      "lastName": "User",
                      "password": "short"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
    }
}

