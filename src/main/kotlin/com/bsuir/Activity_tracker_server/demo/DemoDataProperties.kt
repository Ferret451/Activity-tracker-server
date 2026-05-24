package com.bsuir.Activity_tracker_server.demo

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.demo.seed")
data class DemoDataProperties(
    val email: String = "demo@example.com",
    val password: String = "Demo1234",
    val username: String = "demo_user",
    val firstName: String = "Демо",
    val lastName: String = "Пользователь",
    val historyDays: Int = 7,
)
