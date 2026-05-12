package com.bsuir.Activity_tracker_server.settings

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/me/settings")
class SettingsController(
    private val userSettingsService: UserSettingsService,
) {

    @GetMapping
    fun get(@AuthenticationPrincipal jwt: Jwt): UserSettingsResponse =
        userSettingsService.getSettings(UUID.fromString(jwt.subject))

    @PutMapping
    fun put(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UserSettingsUpdateRequest,
    ): UserSettingsResponse = userSettingsService.putSettings(UUID.fromString(jwt.subject), request)
}
