package com.bsuir.Activity_tracker_server.profile

import com.bsuir.Activity_tracker_server.auth.AuthService
import com.bsuir.Activity_tracker_server.auth.ChangeOwnPasswordRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/me")
class ProfileController(
    private val profileService: ProfileService,
    private val authService: AuthService,
) {

    @GetMapping("/profile")
    fun getProfile(@AuthenticationPrincipal jwt: Jwt): UserProfileResponse =
        profileService.getProfile(extractUserId(jwt))

    @PutMapping("/profile")
    fun upsertProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpsertProfileRequest,
    ): UserProfileResponse = profileService.upsertProfile(extractUserId(jwt), request)

    @GetMapping("/bmr")
    fun calculateBmr(@AuthenticationPrincipal jwt: Jwt): BmrResponse =
        profileService.calculateBmr(extractUserId(jwt))

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: ChangeOwnPasswordRequest,
    ) {
        authService.changeOwnPassword(UUID.fromString(jwt.subject), request)
    }

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}
