package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SecurityIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `protected endpoints reject anonymous requests`() {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/me/profile"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/foods/search").param("query", "apple"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoints allow bearer token`() {
        val accessToken = registerUser("security").accessToken

        mockMvc.perform(get("/api/users").withBearer(accessToken))
            .andExpect(status().isOk)
    }
}

