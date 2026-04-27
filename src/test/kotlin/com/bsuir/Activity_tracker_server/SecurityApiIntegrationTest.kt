package com.bsuir.Activity_tracker_server

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SecurityApiIntegrationTest : ApiFunctionalCoverageIntegrationTest() {

    @Test
    fun `protected endpoints reject anonymous requests`() {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/me/profile"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/foods/search").param("query", "apple"))
            .andExpect(status().isUnauthorized)
    }
}
