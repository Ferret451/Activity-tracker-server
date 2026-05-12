package com.bsuir.Activity_tracker_server.userdata

import com.bsuir.Activity_tracker_server.bodymetrics.CreateWeightEntryRequest
import com.bsuir.Activity_tracker_server.bodymetrics.StepService
import com.bsuir.Activity_tracker_server.bodymetrics.UpdateWeightEntryRequest
import com.bsuir.Activity_tracker_server.bodymetrics.UpsertDailyStepsRequest
import com.bsuir.Activity_tracker_server.bodymetrics.WeightService
import com.bsuir.Activity_tracker_server.common.AccessPolicy
import com.bsuir.Activity_tracker_server.profile.ProfileService
import com.bsuir.Activity_tracker_server.profile.UpsertProfileRequest
import com.bsuir.Activity_tracker_server.settings.UserSettingsService
import com.bsuir.Activity_tracker_server.settings.UserSettingsUpdateRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
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
@RequestMapping("/api/users/{userId}")
class UserWellbeingController(
    private val profileService: ProfileService,
    private val userSettingsService: UserSettingsService,
    private val weightService: WeightService,
    private val stepService: StepService,
    private val accessPolicy: AccessPolicy,
) {

    @GetMapping("/profile")
    fun getProfile(
        @PathVariable userId: UUID,
        authentication: Authentication,
    ) = doWithAccess(authentication, userId) { profileService.getProfile(userId) }

    @PutMapping("/profile")
    fun upsertProfile(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: UpsertProfileRequest,
    ) = doWithAccess(authentication, userId) { profileService.upsertProfile(userId, request) }

    @GetMapping("/bmr")
    fun bmr(
        @PathVariable userId: UUID,
        authentication: Authentication,
    ) = doWithAccess(authentication, userId) { profileService.calculateBmr(userId) }

    @GetMapping("/settings")
    fun getSettings(
        @PathVariable userId: UUID,
        authentication: Authentication,
    ) = doWithAccess(authentication, userId) { userSettingsService.getSettings(userId) }

    @PutMapping("/settings")
    fun putSettings(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: UserSettingsUpdateRequest,
    ) = doWithAccess(authentication, userId) { userSettingsService.putSettings(userId, request) }

    @PostMapping("/weight/entries")
    @ResponseStatus(HttpStatus.CREATED)
    fun createWeight(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateWeightEntryRequest,
    ) = doWithAccess(authentication, userId) { weightService.createEntry(userId, request) }

    @GetMapping("/weight/entries")
    fun listWeight(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ) = doWithAccess(authentication, userId) { weightService.listEntries(userId, from, to) }

    @GetMapping("/weight/entries/{entryId}")
    fun getWeight(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable entryId: UUID,
    ) = doWithAccess(authentication, userId) { weightService.getEntry(userId, entryId) }

    @PutMapping("/weight/entries/{entryId}")
    fun updateWeight(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable entryId: UUID,
        @Valid @RequestBody request: UpdateWeightEntryRequest,
    ) = doWithAccess(authentication, userId) { weightService.updateEntry(userId, entryId, request) }

    @DeleteMapping("/weight/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteWeight(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable entryId: UUID,
    ) {
        doWithAccess(authentication, userId) { weightService.deleteEntry(userId, entryId) }
    }

    @GetMapping("/weight/stats")
    fun weightStats(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ) = doWithAccess(authentication, userId) { weightService.getStats(userId, from, to) }

    @PutMapping("/steps/{date}")
    fun upsertSteps(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable date: LocalDate,
        @Valid @RequestBody request: UpsertDailyStepsRequest,
    ) = doWithAccess(authentication, userId) { stepService.upsertSteps(userId, date, request) }

    @GetMapping("/steps/daily")
    fun dailySteps(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam date: LocalDate,
    ) = doWithAccess(authentication, userId) { stepService.getDaily(userId, date) }

    @GetMapping("/steps/stats")
    fun stepsStats(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ) = doWithAccess(authentication, userId) { stepService.getStats(userId, from, to) }

    private inline fun <T> doWithAccess(authentication: Authentication, userId: UUID, block: () -> T): T {
        accessPolicy.requireSelfOrAdmin(authentication, userId)
        return block()
    }
}
