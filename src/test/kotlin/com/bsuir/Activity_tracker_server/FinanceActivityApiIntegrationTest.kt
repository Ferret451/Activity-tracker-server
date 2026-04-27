package com.bsuir.Activity_tracker_server

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FinanceActivityApiIntegrationTest : ApiFunctionalCoverageIntegrationTest() {

    @Test
    fun `finance and activity endpoints work end-to-end`() {
        val accessToken = register("finance-activity-user").accessToken

        val financeCategoryId = extractId(
            mockMvc.perform(
                post("/api/me/finance/categories")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Salary","operationType":"INCOME"}"""),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").isNotEmpty)
                .andReturn()
                .response
                .contentAsString,
        )

        mockMvc.perform(get("/api/me/finance/categories").withBearer(accessToken))
            .andExpect(status().isOk)

        val financeRecordId = extractId(
            mockMvc.perform(
                post("/api/me/finance/records")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "categoryId": "$financeCategoryId",
                          "amount": 2500.00,
                          "note": "Monthly salary",
                          "occurredAt": "2026-04-20T10:00:00Z"
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

        mockMvc.perform(get("/api/me/finance/records").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/me/finance/records/$financeRecordId").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(financeRecordId))

        mockMvc.perform(
            put("/api/me/finance/records/$financeRecordId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": "$financeCategoryId",
                      "amount": 2600.00,
                      "note": "Updated salary",
                      "occurredAt": "2026-04-20T10:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(2600.00))

        mockMvc.perform(get("/api/me/finance/summary").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome").isNotEmpty)

        mockMvc.perform(delete("/api/me/finance/records/$financeRecordId").withBearer(accessToken))
            .andExpect(status().isNoContent)

        val activityTypeId = extractId(
            mockMvc.perform(
                post("/api/me/activity/types")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Running","met":8.0}"""),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").isNotEmpty)
                .andReturn()
                .response
                .contentAsString,
        )

        mockMvc.perform(get("/api/me/activity/types").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(
            put("/api/me/activity/types/$activityTypeId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Running Updated","met":7.8}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Running Updated"))

        val activityRecordId = extractId(
            mockMvc.perform(
                post("/api/me/activity/records")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "activityTypeId": "$activityTypeId",
                          "durationMinutes": 45,
                          "distanceKm": 7.2,
                          "note": "Morning run",
                          "occurredAt": "2026-04-21T06:30:00Z"
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

        mockMvc.perform(get("/api/me/activity/records").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/me/activity/records/$activityRecordId").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(activityRecordId))

        mockMvc.perform(
            put("/api/me/activity/records/$activityRecordId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "activityTypeId": "$activityTypeId",
                      "durationMinutes": 50,
                      "distanceKm": 8.0,
                      "note": "Updated run",
                      "occurredAt": "2026-04-21T06:30:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.durationMinutes").value(50))

        mockMvc.perform(get("/api/me/activity/summary").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalDurationMinutes").isNumber)

        mockMvc.perform(delete("/api/me/activity/records/$activityRecordId").withBearer(accessToken))
            .andExpect(status().isNoContent)

        mockMvc.perform(delete("/api/me/activity/types/$activityTypeId").withBearer(accessToken))
            .andExpect(status().isNoContent)
    }
}
