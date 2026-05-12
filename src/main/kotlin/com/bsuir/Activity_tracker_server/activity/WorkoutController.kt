package com.bsuir.Activity_tracker_server.activity

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/me/activity/workouts")
@Validated
class WorkoutController(
    private val workoutService: WorkoutService,
) {

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    fun start(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: StartWorkoutRequest,
    ): WorkoutSessionResponse = workoutService.startWorkout(extractUserId(jwt), request)

    @GetMapping
    fun list(@AuthenticationPrincipal jwt: Jwt): List<WorkoutSessionResponse> =
        workoutService.listSessions(extractUserId(jwt))

    @GetMapping("/{sessionId}")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
    ): WorkoutSessionResponse = workoutService.getSession(extractUserId(jwt), sessionId)

    @PostMapping("/{sessionId}/route-points")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun appendRoutePoints(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
        @Valid @RequestBody request: AppendWorkoutRoutePointsRequest,
    ) {
        workoutService.appendRoutePoints(extractUserId(jwt), sessionId, request)
    }

    @GetMapping("/{sessionId}/route")
    fun getRoute(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
    ): List<WorkoutRoutePointResponse> = workoutService.getRoute(extractUserId(jwt), sessionId)

    @PostMapping("/{sessionId}/stop")
    fun stop(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
        @Valid @RequestBody(required = false) request: StopWorkoutRequest?,
    ): WorkoutSessionResponse = workoutService.stopWorkout(extractUserId(jwt), sessionId, request)

    @PostMapping("/{sessionId}/cancel")
    fun cancel(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sessionId: UUID,
    ): WorkoutSessionResponse = workoutService.cancelWorkout(extractUserId(jwt), sessionId)

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}
