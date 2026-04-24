package com.bsuir.Activity_tracker_server.goal

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/me/goals")
class GoalController(
    private val goalService: GoalService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createGoal(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateGoalRequest,
    ): UserGoalResponse = goalService.createGoal(extractUserId(jwt), request)

    @GetMapping
    fun getGoals(@AuthenticationPrincipal jwt: Jwt): List<UserGoalResponse> =
        goalService.getGoals(extractUserId(jwt))

    @GetMapping("/{goalId}")
    fun getGoal(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable goalId: UUID,
    ): UserGoalResponse = goalService.getGoal(extractUserId(jwt), goalId)

    @PutMapping("/{goalId}/status")
    fun updateGoalStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable goalId: UUID,
        @Valid @RequestBody request: UpdateGoalStatusRequest,
    ): UserGoalResponse = goalService.updateStatus(extractUserId(jwt), goalId, request)

    @PostMapping("/{goalId}/progress")
    fun addGoalProgress(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable goalId: UUID,
        @Valid @RequestBody request: AddGoalProgressRequest,
    ): UserGoalResponse = goalService.addProgress(extractUserId(jwt), goalId, request)

    @GetMapping("/{goalId}/progress")
    fun getGoalProgress(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable goalId: UUID,
    ): List<GoalProgressResponse> = goalService.getProgressEntries(extractUserId(jwt), goalId)

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}

