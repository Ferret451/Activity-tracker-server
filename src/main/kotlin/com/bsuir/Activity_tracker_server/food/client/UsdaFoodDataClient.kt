package com.bsuir.Activity_tracker_server.food.client

import com.bsuir.Activity_tracker_server.food.exception.ExternalFoodApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestClient
import java.nio.charset.StandardCharsets

@Component
class UsdaFoodDataClient(
    @Value("\${app.food.usda.base-url:https://api.nal.usda.gov/fdc/v1}")
    private val baseUrl: String,
    @Value("\${app.food.usda.api-key}")
    private val apiKey: String,
) : FoodDataClient {

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    override fun searchFoods(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): UsdaSearchResponse {
        ensureApiKeyConfigured()

        return restClient.get()
            .uri { builder ->
                val uriBuilder = builder
                    .path("/foods/search")
                    .queryParam("query", query)
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", pageSize)
                    .queryParam("api_key", apiKey)

                if (dataTypes.isNotEmpty()) {
                    uriBuilder.queryParam("dataType", *dataTypes.toTypedArray())
                }
                if (!sortBy.isNullOrBlank()) {
                    uriBuilder.queryParam("sortBy", sortBy)
                }
                if (!sortOrder.isNullOrBlank()) {
                    uriBuilder.queryParam("sortOrder", sortOrder)
                }
                if (!brandOwner.isNullOrBlank()) {
                    uriBuilder.queryParam("brandOwner", brandOwner)
                }

                uriBuilder.build()
            }
            .header("X-Api-Key", apiKey)
            .retrieve()
            .onStatus(HttpStatusCode::isError, ::throwMappedException)
            .body(UsdaSearchResponse::class.java)
            ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA search response is empty")
    }

    override fun getFoodById(fdcId: Long): UsdaFoodItemResponse {
        ensureApiKeyConfigured()

        return restClient.get()
            .uri { builder ->
                builder.path("/food/{fdcId}")
                    .queryParam("api_key", apiKey)
                    .build(fdcId)
            }
            .header("X-Api-Key", apiKey)
            .retrieve()
            .onStatus(HttpStatusCode::isError, ::throwMappedException)
            .body(UsdaFoodItemResponse::class.java)
            ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA food details response is empty")
    }

    override fun getFoodsByIds(fdcIds: List<Long>): List<UsdaFoodItemResponse> {
        ensureApiKeyConfigured()

        val ids = fdcIds.joinToString(",")

        return restClient.get()
            .uri { builder ->
                builder.path("/foods")
                    .queryParam("fdcIds", ids)
                    .queryParam("api_key", apiKey)
                    .build()
            }
            .header("X-Api-Key", apiKey)
            .retrieve()
            .onStatus(HttpStatusCode::isError, ::throwMappedException)
            .body(Array<UsdaFoodItemResponse>::class.java)
            ?.toList()
            ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA foods response is empty")
    }

    private fun ensureApiKeyConfigured() {
        check(apiKey.isNotBlank()) { "FOOD_API_KEY is not configured" }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun throwMappedException(_request: HttpRequest, response: ClientHttpResponse) {
        val body = StreamUtils.copyToString(response.body, StandardCharsets.UTF_8)
        throw ExternalFoodApiException(response.statusCode, body)
    }
}



