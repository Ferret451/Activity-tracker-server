package com.bsuir.Activity_tracker_server.food.client

import com.bsuir.Activity_tracker_server.food.exception.ExternalFoodApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestClient
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

@Component
class UsdaFoodDataClient(
    @Value("\${app.food.usda.base-url:https://api.nal.usda.gov/fdc/v1}")
    private val baseUrl: String,
    @Value("\${app.food.usda.api-key}")
    private val apiKey: String,
    @Value("\${app.food.usda.timeout.connect-ms:3000}")
    connectTimeoutMs: Int,
    @Value("\${app.food.usda.timeout.read-ms:5000}")
    readTimeoutMs: Int,
    @Value("\${app.food.usda.retry.max-attempts:3}")
    retryMaxAttempts: Int,
    @Value("\${app.food.usda.retry.initial-backoff-ms:300}")
    retryInitialBackoffMs: Long,
    @Value("\${app.food.usda.retry.max-backoff-ms:3000}")
    retryMaxBackoffMs: Long,
    @Value("\${app.food.usda.retry.backoff-multiplier:2.0}")
    retryBackoffMultiplier: Double,
    @Value("\${app.food.usda.circuit-breaker.failure-threshold:5}")
    circuitFailureThreshold: Int,
    @Value("\${app.food.usda.circuit-breaker.open-ms:30000}")
    circuitOpenMs: Long,
) : FoodDataClient {

    private val retryMaxAttempts = retryMaxAttempts.coerceAtLeast(1)
    private val retryInitialBackoffMs = retryInitialBackoffMs.coerceAtLeast(0)
    private val retryMaxBackoffMs = retryMaxBackoffMs.coerceAtLeast(this.retryInitialBackoffMs)
    private val retryBackoffMultiplier = retryBackoffMultiplier.coerceAtLeast(1.0)
    private val circuitFailureThreshold = circuitFailureThreshold.coerceAtLeast(1)
    private val circuitOpenMs = circuitOpenMs.coerceAtLeast(1)

    private val consecutiveFailures = AtomicInteger(0)

    @Volatile
    private var circuitOpenUntilEpochMs: Long = 0

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(connectTimeoutMs.coerceAtLeast(100))
                setReadTimeout(readTimeoutMs.coerceAtLeast(100))
            },
        )
        .build()

    override fun searchFoodsGet(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): UsdaSearchResponse {
        ensureApiKeyConfigured()

        return executeWithResilience("searchFoodsGet") {
            restClient.get()
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
    }

    override fun searchFoodsPost(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): UsdaSearchResponse {
        ensureApiKeyConfigured()

        return executeWithResilience("searchFoodsPost") {
            restClient.post()
                .uri { builder ->
                    builder.path("/foods/search")
                        .queryParam("api_key", apiKey)
                        .build()
                }
                .header("X-Api-Key", apiKey)
                .body(
                    UsdaSearchRequest(
                        query = query,
                        dataType = dataTypes,
                        pageSize = pageSize,
                        pageNumber = pageNumber,
                        sortBy = sortBy,
                        sortOrder = sortOrder,
                        brandOwner = brandOwner,
                    ),
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, ::throwMappedException)
                .body(UsdaSearchResponse::class.java)
                ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA search response is empty")
        }
    }

    override fun listFoodsGet(
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
    ): List<UsdaFoodItemResponse> {
        ensureApiKeyConfigured()

        return executeWithResilience("listFoodsGet") {
            restClient.get()
                .uri { builder ->
                    val uriBuilder = builder
                        .path("/foods/list")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .queryParam("api_key", apiKey)

                    if (dataTypes.isNotEmpty()) {
                        uriBuilder.queryParam("dataType", *dataTypes.toTypedArray())
                    }
                    if (!sortBy.isNullOrBlank()) {
                        uriBuilder.queryParam("sortBy", sortBy)
                    }

                    uriBuilder.build()
                }
                .header("X-Api-Key", apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ::throwMappedException)
                .body(Array<UsdaFoodItemResponse>::class.java)
                ?.toList()
                ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA foods list response is empty")
        }
    }

    override fun listFoodsPost(
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
    ): List<UsdaFoodItemResponse> {
        ensureApiKeyConfigured()

        return executeWithResilience("listFoodsPost") {
            restClient.post()
                .uri { builder ->
                    builder.path("/foods/list")
                        .queryParam("api_key", apiKey)
                        .build()
                }
                .header("X-Api-Key", apiKey)
                .body(
                    UsdaFoodsListRequest(
                        dataType = dataTypes,
                        pageSize = pageSize,
                        pageNumber = pageNumber,
                        sortBy = sortBy,
                    ),
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, ::throwMappedException)
                .body(Array<UsdaFoodItemResponse>::class.java)
                ?.toList()
                ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA foods list response is empty")
        }
    }

    override fun getFoodById(
        fdcId: Long,
        format: String?,
        nutrients: List<Long>,
    ): UsdaFoodItemResponse {
        ensureApiKeyConfigured()

        return executeWithResilience("getFoodById") {
            restClient.get()
                .uri { builder ->
                    val uriBuilder = builder
                        .path("/food/{fdcId}")
                        .queryParam("api_key", apiKey)
                    appendFormatAndNutrients(uriBuilder, format, nutrients)
                    uriBuilder.build(fdcId)
                }
                .header("X-Api-Key", apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ::throwMappedException)
                .body(UsdaFoodItemResponse::class.java)
                ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA food details response is empty")
        }
    }

    override fun getFoodsByIdsGet(
        fdcIds: List<Long>,
        format: String?,
        nutrients: List<Long>,
    ): List<UsdaFoodItemResponse> {
        ensureApiKeyConfigured()

        val ids = fdcIds.joinToString(",")

        return executeWithResilience("getFoodsByIdsGet") {
            restClient.get()
                .uri { builder ->
                    val uriBuilder = builder
                        .path("/foods")
                        .queryParam("fdcIds", ids)
                        .queryParam("api_key", apiKey)
                    appendFormatAndNutrients(uriBuilder, format, nutrients)
                    uriBuilder.build()
                }
                .header("X-Api-Key", apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ::throwMappedException)
                .body(Array<UsdaFoodItemResponse>::class.java)
                ?.toList()
                ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA foods response is empty")
        }
    }

    override fun getFoodsByIdsPost(
        fdcIds: List<Long>,
        format: String?,
        nutrients: List<Long>,
    ): List<UsdaFoodItemResponse> {
        ensureApiKeyConfigured()

        return executeWithResilience("getFoodsByIdsPost") {
            restClient.post()
                .uri { builder ->
                    builder.path("/foods")
                        .queryParam("api_key", apiKey)
                        .build()
                }
                .header("X-Api-Key", apiKey)
                .body(
                    UsdaFoodsRequest(
                        fdcIds = fdcIds,
                        format = format,
                        nutrients = nutrients,
                    ),
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, ::throwMappedException)
                .body(Array<UsdaFoodItemResponse>::class.java)
                ?.toList()
                ?: throw ExternalFoodApiException(HttpStatus.BAD_GATEWAY, "USDA foods response is empty")
        }
    }

    private fun ensureApiKeyConfigured() {
        if (apiKey.isBlank()) {
            throw ExternalFoodApiException(
                statusCode = HttpStatus.SERVICE_UNAVAILABLE,
                responseBody = "FOOD_API_KEY is not configured",
            )
        }
    }

    private fun appendFormatAndNutrients(
        builder: org.springframework.web.util.UriBuilder,
        format: String?,
        nutrients: List<Long>,
    ) {
        if (!format.isNullOrBlank()) {
            builder.queryParam("format", format)
        }
        if (nutrients.isNotEmpty()) {
            builder.queryParam("nutrients", nutrients.joinToString(","))
        }
    }

    private fun <T> executeWithResilience(callName: String, operation: () -> T): T {
        ensureCircuitClosed(callName)

        var attempt = 1
        var backoffMs = retryInitialBackoffMs

        while (true) {
            try {
                val result = operation()
                onCallSuccess()
                return result
            } catch (exception: ExternalFoodApiException) {
                val status = exception.statusCode.value()
                val retryable = status == 429 || status >= 500
                if (retryable && attempt < retryMaxAttempts) {
                    sleepSafely(backoffMs)
                    attempt += 1
                    backoffMs = nextBackoff(backoffMs)
                    continue
                }
                onCallFailure(status)
                throw exception
            } catch (exception: Exception) {
                if (attempt < retryMaxAttempts) {
                    sleepSafely(backoffMs)
                    attempt += 1
                    backoffMs = nextBackoff(backoffMs)
                    continue
                }

                onCallFailure(HttpStatus.BAD_GATEWAY.value())
                throw ExternalFoodApiException(
                    statusCode = HttpStatus.BAD_GATEWAY,
                    responseBody = "USDA request failed: ${exception.message}",
                )
            }
        }
    }

    private fun nextBackoff(currentMs: Long): Long {
        val next = (currentMs * retryBackoffMultiplier).toLong()
        return next.coerceAtMost(retryMaxBackoffMs)
    }

    private fun sleepSafely(delayMs: Long) {
        if (delayMs <= 0) {
            return
        }
        try {
            Thread.sleep(delayMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ExternalFoodApiException(
                statusCode = HttpStatus.SERVICE_UNAVAILABLE,
                responseBody = "USDA retry was interrupted",
            )
        }
    }

    private fun ensureCircuitClosed(callName: String) {
        val now = System.currentTimeMillis()
        if (now < circuitOpenUntilEpochMs) {
            val remainingSeconds = ((circuitOpenUntilEpochMs - now) / 1000L).toInt().coerceAtLeast(1)
            throw ExternalFoodApiException(
                statusCode = HttpStatus.SERVICE_UNAVAILABLE,
                responseBody = "USDA API circuit breaker is open for call '$callName'. Retry after $remainingSeconds second(s)",
            )
        }
    }

    private fun onCallSuccess() {
        consecutiveFailures.set(0)
        circuitOpenUntilEpochMs = 0
    }

    private fun onCallFailure(statusCode: Int) {
        if (statusCode != 429 && statusCode < 500) {
            return
        }

        val failures = consecutiveFailures.incrementAndGet()
        if (failures >= circuitFailureThreshold) {
            circuitOpenUntilEpochMs = System.currentTimeMillis() + circuitOpenMs
            consecutiveFailures.set(0)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun throwMappedException(_request: HttpRequest, response: ClientHttpResponse) {
        val body = StreamUtils.copyToString(response.body, StandardCharsets.UTF_8)
        throw ExternalFoodApiException(
            statusCode = response.statusCode,
            responseBody = body,
        )
    }
}

