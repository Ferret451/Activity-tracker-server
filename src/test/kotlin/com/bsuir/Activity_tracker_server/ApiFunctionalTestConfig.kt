package com.bsuir.Activity_tracker_server

import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class ApiFunctionalTestConfig {

    @Bean
    @Primary
    fun foodDataClient(): FoodDataClient = mock(FoodDataClient::class.java)
}

