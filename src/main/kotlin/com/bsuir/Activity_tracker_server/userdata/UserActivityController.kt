package com.bsuir.Activity_tracker_server.userdata

import com.bsuir.Activity_tracker_server.activity.ActivityQueryFilter
import com.bsuir.Activity_tracker_server.activity.ActivityService
import com.bsuir.Activity_tracker_server.activity.AppendWorkoutRoutePointsRequest
import com.bsuir.Activity_tracker_server.activity.CreateActivityRecordRequest
import com.bsuir.Activity_tracker_server.activity.StartWorkoutRequest
import com.bsuir.Activity_tracker_server.activity.StopWorkoutRequest
import com.bsuir.Activity_tracker_server.activity.UpdateActivityRecordRequest
import com.bsuir.Activity_tracker_server.activity.WorkoutService
import com.bsuir.Activity_tracker_server.common.AccessPolicy
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/activity")
@Validated
class UserActivityController(
    private val activityService: ActivityService,
    private val workoutService: WorkoutService,
    private val accessPolicy: AccessPolicy,
) {

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateActivityRecordRequest,
    ) = doWithAccess(authentication, userId) { activityService.createRecord(userId, request) }

    @GetMapping("/records")
    fun getRecords(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) activityTypeId: UUID?,
    ) = doWithAccess(authentication, userId) {
        activityService.getRecords(
            userId,
            ActivityQueryFilter(from = from, to = to, activityTypeId = activityTypeId),
        )
    }

    @GetMapping("/records/{recordId}")
    fun getRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
    ) = doWithAccess(authentication, userId) { activityService.getRecord(userId, recordId) }

    @PutMapping("/records/{recordId}")
    fun updateRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateActivityRecordRequest,
    ) = doWithAccess(authentication, userId) { activityService.updateRecord(userId, recordId, request) }

    @DeleteMapping("/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
    ) {
        doWithAccess(authentication, userId) { activityService.deleteRecord(userId, recordId) }
    }

    @GetMapping("/summary")
    fun getSummary(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) activityTypeId: UUID?,
    ) = doWithAccess(authentication, userId) {
        activityService.getSummary(
            userId,
            ActivityQueryFilter(from = from, to = to, activityTypeId = activityTypeId),
        )
    }

    @PostMapping("/workouts/start")
    @ResponseStatus(HttpStatus.CREATED)
    fun startWorkout(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: StartWorkoutRequest,
    ) = doWithAccess(authentication, userId) { workoutService.startWorkout(userId, request) }

    @GetMapping("/workouts")
    fun listWorkouts(
        @PathVariable userId: UUID,
        authentication: Authentication,
    ) = doWithAccess(authentication, userId) { workoutService.listSessions(userId) }

    @GetMapping("/workouts/{sessionId}")
    fun getWorkout(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable sessionId: UUID,
    ) = doWithAccess(authentication, userId) { workoutService.getSession(userId, sessionId) }

    @PostMapping("/workouts/{sessionId}/route-points")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun appendRoutePoints(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable sessionId: UUID,
        @Valid @RequestBody request: AppendWorkoutRoutePointsRequest,
    ) {
        doWithAccess(authentication, userId) { workoutService.appendRoutePoints(userId, sessionId, request) }
    }

    @GetMapping("/workouts/{sessionId}/route")
    fun getRoute(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable sessionId: UUID,
    ) = doWithAccess(authentication, userId) { workoutService.getRoute(userId, sessionId) }

    @PostMapping("/workouts/{sessionId}/stop")
    fun stopWorkout(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable sessionId: UUID,
        @Valid @RequestBody(required = false) request: StopWorkoutRequest?,
    ) = doWithAccess(authentication, userId) { workoutService.stopWorkout(userId, sessionId, request) }

    @PostMapping("/workouts/{sessionId}/cancel")
    fun cancelWorkout(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable sessionId: UUID,
    ) = doWithAccess(authentication, userId) { workoutService.cancelWorkout(userId, sessionId) }

    private inline fun <T> doWithAccess(authentication: Authentication, userId: UUID, block: () -> T): T {
        accessPolicy.requireSelfOrAdmin(authentication, userId)
        return block()
    }
}
