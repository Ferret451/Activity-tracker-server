package com.bsuir.Activity_tracker_server.avatar

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.storage.avatars.local")
data class AvatarProperties(
    val baseDir: String,
    val publicBaseUrl: String,
)

