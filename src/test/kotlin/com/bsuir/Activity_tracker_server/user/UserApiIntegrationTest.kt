package com.bsuir.Activity_tracker_server.user

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class UserApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `users CRUD works`() {
        val accessToken = registerUser("admin").accessToken

        val createdId = mockMvc.perform(
            post("/api/users")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "managed-${UUID.randomUUID()}@example.com",
                      "username": "managed_${UUID.randomUUID().toString().substring(0, 8)}",
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
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(get("/api/users").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/users/$createdId").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(createdId))

        mockMvc.perform(
            put("/api/users/$createdId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "managed-upd-${UUID.randomUUID()}@example.com",
                      "username": "managed_upd_${UUID.randomUUID().toString().substring(0, 8)}",
                      "firstName": "Updated",
                      "lastName": "User"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Updated"))

        mockMvc.perform(delete("/api/users/$createdId").withBearer(accessToken))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `users validation returns 400`() {
        val accessToken = registerUser("val").accessToken

        mockMvc.perform(
            post("/api/users")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "not-an-email",
                      "username": "",
                      "firstName": "",
                      "lastName": "",
                      "password": "short"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
    }
}

