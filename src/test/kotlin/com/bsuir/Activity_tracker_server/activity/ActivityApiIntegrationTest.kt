package com.bsuir.Activity_tracker_server.activity

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ActivityApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `activity types and records flow works with summary`() {
        val accessToken = registerUser("activity").accessToken

        mockMvc.perform(
            put("/api/me/profile")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "weightKg": 70.0,
                      "heightCm": 180,
                      "age": 25,
                      "sex": "MALE",
                      "activityLevel": "MODERATE",
                      "calorieGoal": "MAINTAIN"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        val typesNode = mockMvc.perform(get("/api/me/activity/types").withBearer(accessToken))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it) }

        assertTrue(typesNode.size() >= 20, "Expected at least 20 default activity types")
        val typeNames = typesNode.map { it.path("name").asText() }.toSet()
        assertTrue("Walking" in typeNames)
        assertTrue("Running, moderate" in typeNames)
        assertTrue("Swimming, leisure" in typeNames)
        assertTrue("Sitting" in typeNames)

        val walkingType = typesNode.first { it.path("name").asText() == "Walking" }
        assertTrue(walkingType.path("supportsDistance").asBoolean())
        val yogaType = typesNode.first { it.path("name").asText() == "Yoga" }
        assertTrue(!yogaType.path("supportsDistance").asBoolean())

        val typeId = walkingType.path("id").asText()

        val recordId = mockMvc.perform(
            post("/api/me/activity/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"activityTypeId":"$typeId","durationMinutes":30,"distanceKm":5.0,"note":"test"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.burnedCaloriesKcal").isNumber)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(get("/api/me/activity/records").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(recordId))

        mockMvc.perform(get("/api/me/activity/summary").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalDurationMinutes").value(30))
            .andExpect(jsonPath("$.byType[0].activityTypeId").value(typeId))

        mockMvc.perform(delete("/api/me/activity/records/$recordId").withBearer(accessToken))
            .andExpect(status().isNoContent)

        val adminToken = registerAdminUser("actypeadmin").accessToken
        val customTypeId = mockMvc.perform(
            post("/api/me/activity/types")
                .withBearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"TestRun-${System.nanoTime()}","met":8.0,"supportsDistance":true}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(
            put("/api/me/activity/types/$customTypeId")
                .withBearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"TestRunUpd-${System.nanoTime()}","met":7.5,"supportsDistance":false}"""),
        )
            .andExpect(status().isOk)

        mockMvc.perform(delete("/api/me/activity/types/$customTypeId").withBearer(adminToken))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `record with distance rejected for type without distance support`() {
        val accessToken = registerUser("activitynodist").accessToken

        mockMvc.perform(
            put("/api/me/profile")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "weightKg": 70.0,
                      "heightCm": 180,
                      "age": 25,
                      "sex": "MALE",
                      "activityLevel": "MODERATE",
                      "calorieGoal": "MAINTAIN"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        val yogaId = mockMvc.perform(get("/api/me/activity/types").withBearer(accessToken))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it) }
            .first { it.path("name").asText() == "Yoga" }
            .path("id")
            .asText()

        mockMvc.perform(
            post("/api/me/activity/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"activityTypeId":"$yogaId","durationMinutes":30,"distanceKm":1.0}"""),
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/api/me/activity/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"activityTypeId":"$yogaId","durationMinutes":30}"""),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `workout route points rejected for type without distance support`() {
        val accessToken = registerUser("workoutnodist").accessToken

        mockMvc.perform(
            put("/api/me/profile")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "weightKg": 70.0,
                      "heightCm": 180,
                      "age": 25,
                      "sex": "MALE",
                      "activityLevel": "MODERATE",
                      "calorieGoal": "MAINTAIN"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        val types = mockMvc.perform(get("/api/me/activity/types").withBearer(accessToken))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it) }

        val yogaId = types.first { it.path("name").asText() == "Yoga" }.path("id").asText()
        val joggingId = types.first { it.path("name").asText() == "Jogging" }.path("id").asText()

        val yogaSessionId = mockMvc.perform(
            post("/api/me/activity/workouts/start")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"activityTypeId":"$yogaId"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(
            post("/api/me/activity/workouts/$yogaSessionId/route-points")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"points":[{"latitude":53.9,"longitude":27.5}]}"""),
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/api/me/activity/workouts/$yogaSessionId/cancel")
                .withBearer(accessToken),
        )
            .andExpect(status().isOk)

        val joggingSessionId = mockMvc.perform(
            post("/api/me/activity/workouts/start")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"activityTypeId":"$joggingId"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(
            post("/api/me/activity/workouts/$joggingSessionId/route-points")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"points":[{"latitude":53.9,"longitude":27.5}]}"""),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/me/activity/workouts/$joggingSessionId/stop")
                .withBearer(accessToken),
        )
            .andExpect(status().isOk)
    }
}
