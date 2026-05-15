package com.bsuir.Activity_tracker_server.bodymetrics

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class StepsApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `upsert daily get and stats return steps calories and walking duration`() {
        val accessToken = registerUser("steps").accessToken
        val date = LocalDate.of(2026, 5, 15)
        val prevDate = date.minusDays(1)

        mockMvc.perform(
            put("/api/me/steps/$date")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "steps": 8500,
                      "burnedCaloriesKcal": 320.5,
                      "walkingDurationMinutes": 75
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.date").value(date.toString()))
            .andExpect(jsonPath("$.steps").value(8500))
            .andExpect(jsonPath("$.burnedCaloriesKcal").value(320.5))
            .andExpect(jsonPath("$.walkingDurationMinutes").value(75))
            .andExpect(jsonPath("$.createdAt").isNotEmpty)
            .andExpect(jsonPath("$.updatedAt").isNotEmpty)

        mockMvc.perform(
            get("/api/me/steps/daily")
                .withBearer(accessToken)
                .param("date", date.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.steps").value(8500))
            .andExpect(jsonPath("$.burnedCaloriesKcal").value(320.5))
            .andExpect(jsonPath("$.walkingDurationMinutes").value(75))

        mockMvc.perform(
            get("/api/me/steps/stats")
                .withBearer(accessToken)
                .param("from", prevDate.toString())
                .param("to", date.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.days.length()").value(2))
            .andExpect(jsonPath("$.days[0].date").value(prevDate.toString()))
            .andExpect(jsonPath("$.days[0].steps").value(0))
            .andExpect(jsonPath("$.days[0].burnedCaloriesKcal").value(0.0))
            .andExpect(jsonPath("$.days[0].walkingDurationMinutes").value(0))
            .andExpect(jsonPath("$.days[1].date").value(date.toString()))
            .andExpect(jsonPath("$.days[1].steps").value(8500))
            .andExpect(jsonPath("$.days[1].burnedCaloriesKcal").value(320.5))
            .andExpect(jsonPath("$.days[1].walkingDurationMinutes").value(75))
    }
}
