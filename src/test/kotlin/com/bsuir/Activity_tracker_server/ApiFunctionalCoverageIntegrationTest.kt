package com.bsuir.Activity_tracker_server
import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
@SpringBootTest(properties = ["app.food.usda.api-key=test-key"])
@AutoConfigureMockMvc
@Import(ApiFunctionalTestConfig::class)
abstract class ApiFunctionalCoverageIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var foodDataClient: FoodDataClient
    private val objectMapper = ObjectMapper()
    @BeforeEach
    fun resetUsdaClientMock() {
        reset(foodDataClient)
    }
    protected fun register(prefix: String): RegisteredUser {
        val unique = UUID.randomUUID().toString().substring(0, 8)
        val email = "$prefix-$unique@example.com"
        val username = "${prefix.replace('-', '_')}_$unique"
        val response = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "username": "$username",
                      "firstName": "Test",
                      "lastName": "User",
                      "password": "password123"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.user.id").isNotEmpty)
            .andReturn()
            .response
            .contentAsString
        val json = objectMapper.readTree(response)
        val accessToken = json.path("accessToken").asText()
        val refreshToken = json.path("refreshToken").asText()
        val userId = json.path("user").path("id").asText()
        assertNotNull(accessToken)
        assertNotNull(refreshToken)
        assertNotNull(userId)
        return RegisteredUser(email, accessToken, refreshToken)
    }
    protected fun extractId(jsonBody: String): String = objectMapper.readTree(jsonBody).path("id").asText()
    protected fun MockHttpServletRequestBuilder.withBearer(accessToken: String): MockHttpServletRequestBuilder =
        header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
    protected fun usdaFood(
        fdcId: Long,
        description: String,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
    ): UsdaFoodItemResponse =
        UsdaFoodItemResponse(
            fdcId = fdcId,
            description = description,
            foodNutrients = listOf(
                UsdaFoodNutrient(number = "1008", amount = calories),
                UsdaFoodNutrient(number = "1003", amount = protein),
                UsdaFoodNutrient(number = "1004", amount = fat),
                UsdaFoodNutrient(number = "1005", amount = carbs),
            ),
        )
    protected data class RegisteredUser(
        val email: String,
        val accessToken: String,
        val refreshToken: String,
    )
}


