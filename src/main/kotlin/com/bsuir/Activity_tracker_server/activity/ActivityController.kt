package com.bsuir.Activity_tracker_server.activity

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
@RequestMapping("/api/me/activity")
class ActivityController(
    private val activityService: ActivityService,
) {

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun createType(
        @Valid @RequestBody request: CreateActivityTypeRequest,
    ): ActivityTypeResponse = activityService.createType(request)

    @GetMapping("/types")
    fun getTypes(): List<ActivityTypeResponse> =
        activityService.getTypes()

    @PutMapping("/types/{typeId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun updateType(
        @PathVariable typeId: UUID,
        @Valid @RequestBody request: UpdateActivityTypeRequest,
    ): ActivityTypeResponse = activityService.updateType(typeId, request)

    @DeleteMapping("/types/{typeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun deleteType(
        @PathVariable typeId: UUID,
    ) {
        activityService.deleteType(typeId)
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateActivityRecordRequest,
    ): ActivityRecordResponse = activityService.createRecord(extractUserId(jwt), request)

    @GetMapping("/records")
    fun getRecords(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) activityTypeId: UUID?,
    ): List<ActivityRecordResponse> =
        activityService.getRecords(
            userId = extractUserId(jwt),
            filter = ActivityQueryFilter(
                from = from,
                to = to,
                activityTypeId = activityTypeId,
            ),
        )

    @GetMapping("/records/{recordId}")
    fun getRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ): ActivityRecordResponse = activityService.getRecord(extractUserId(jwt), recordId)

    @PutMapping("/records/{recordId}")
    fun updateRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateActivityRecordRequest,
    ): ActivityRecordResponse = activityService.updateRecord(extractUserId(jwt), recordId, request)

    @DeleteMapping("/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ) {
        activityService.deleteRecord(extractUserId(jwt), recordId)
    }

    @GetMapping("/summary")
    fun getSummary(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) activityTypeId: UUID?,
    ): ActivitySummaryResponse =
        activityService.getSummary(
            userId = extractUserId(jwt),
            filter = ActivityQueryFilter(
                from = from,
                to = to,
                activityTypeId = activityTypeId,
            ),
        )

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}
