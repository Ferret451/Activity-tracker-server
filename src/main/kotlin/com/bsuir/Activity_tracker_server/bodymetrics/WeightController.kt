package com.bsuir.Activity_tracker_server.bodymetrics

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
@RequestMapping("/api/me/weight")
class WeightController(
    private val weightService: WeightService,
) {

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateWeightEntryRequest,
    ): WeightEntryResponse = weightService.createEntry(extractUserId(jwt), request)

    @GetMapping("/entries")
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): List<WeightEntryResponse> = weightService.listEntries(extractUserId(jwt), from, to)

    @GetMapping("/entries/{entryId}")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable entryId: UUID,
    ): WeightEntryResponse = weightService.getEntry(extractUserId(jwt), entryId)

    @PutMapping("/entries/{entryId}")
    fun update(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable entryId: UUID,
        @Valid @RequestBody request: UpdateWeightEntryRequest,
    ): WeightEntryResponse = weightService.updateEntry(extractUserId(jwt), entryId, request)

    @DeleteMapping("/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable entryId: UUID,
    ) {
        weightService.deleteEntry(extractUserId(jwt), entryId)
    }

    @GetMapping("/stats")
    fun stats(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): WeightStatsResponse = weightService.getStats(extractUserId(jwt), from, to)

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}
