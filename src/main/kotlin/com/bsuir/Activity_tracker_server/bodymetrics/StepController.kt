package com.bsuir.Activity_tracker_server.bodymetrics

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/me/steps")
class StepController(
    private val stepService: StepService,
) {

    @PutMapping("/{date}")
    @ResponseStatus(HttpStatus.OK)
    fun upsert(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable date: LocalDate,
        @Valid @RequestBody request: UpsertDailyStepsRequest,
    ): DailyStepsResponse = stepService.upsertSteps(extractUserId(jwt), date, request)

    @GetMapping("/daily")
    fun daily(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam date: LocalDate,
    ): DailyStepsResponse = stepService.getDaily(extractUserId(jwt), date)

    @GetMapping("/stats")
    fun stats(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): DailyStepsStatsResponse = stepService.getStats(extractUserId(jwt), from, to)

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}
