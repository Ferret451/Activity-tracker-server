package com.bsuir.Activity_tracker_server.food.exception

import org.springframework.http.HttpStatusCode

class ExternalFoodApiException(
    val statusCode: HttpStatusCode,
    val responseBody: String?,
) : RuntimeException("USDA Food API request failed with status ${statusCode.value()}")

