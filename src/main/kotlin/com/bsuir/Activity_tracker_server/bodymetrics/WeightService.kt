package com.bsuir.Activity_tracker_server.bodymetrics

import com.bsuir.Activity_tracker_server.profile.UserProfileRepository
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import com.bsuir.Activity_tracker_server.bodymetrics.exception.WeightEntryNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class WeightService(
    private val userRepository: UserRepository,
    private val weightEntryRepository: WeightEntryRepository,
    private val userProfileRepository: UserProfileRepository,
) {

    @Transactional
    fun createEntry(userId: UUID, request: CreateWeightEntryRequest): WeightEntryResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val measuredAt = request.measuredAt ?: OffsetDateTime.now(ZoneOffset.UTC)
        val entry =
            WeightEntry(
                user = user,
                weightKg = request.weightKg,
                measuredAt = measuredAt,
                note = request.note?.trim()?.takeIf(String::isNotBlank),
            )
        val saved = weightEntryRepository.save(entry)
        syncProfileWeightFromLatest(userId)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun listEntries(userId: UUID, from: LocalDate?, to: LocalDate?): List<WeightEntryResponse> {
        userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val entries =
            when {
                from != null && to != null -> {
                    if (from.isAfter(to)) {
                        throw IllegalArgumentException("Parameter 'from' must be before or equal to 'to'")
                    }
                    val fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC)
                    val toExclusive = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
                    weightEntryRepository.findAllByUser_IdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        userId,
                        fromTs,
                        toExclusive,
                    )
                }
                from != null -> {
                    val fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC)
                    weightEntryRepository.findAllByUser_IdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        userId,
                        fromTs,
                        OffsetDateTime.MAX,
                    )
                }
                to != null -> {
                    val toExclusive = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
                    weightEntryRepository.findAllByUser_IdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        userId,
                        OffsetDateTime.MIN,
                        toExclusive,
                    )
                }
                else -> weightEntryRepository.findAllByUser_IdOrderByMeasuredAtDesc(userId).reversed()
            }
        return entries.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getEntry(userId: UUID, entryId: UUID): WeightEntryResponse =
        findOwned(userId, entryId).toResponse()

    @Transactional
    fun updateEntry(userId: UUID, entryId: UUID, request: UpdateWeightEntryRequest): WeightEntryResponse {
        val entry = findOwned(userId, entryId)
        entry.weightKg = request.weightKg
        entry.measuredAt = request.measuredAt
        entry.note = request.note?.trim()?.takeIf(String::isNotBlank)
        val saved = weightEntryRepository.save(entry)
        syncProfileWeightFromLatest(userId)
        return saved.toResponse()
    }

    @Transactional
    fun deleteEntry(userId: UUID, entryId: UUID) {
        val entry = findOwned(userId, entryId)
        weightEntryRepository.delete(entry)
        syncProfileWeightFromLatest(userId)
    }

    @Transactional(readOnly = true)
    fun getStats(userId: UUID, from: LocalDate, to: LocalDate): WeightStatsResponse {
        userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        if (from.isAfter(to)) {
            throw IllegalArgumentException("Parameter 'from' must be before or equal to 'to'")
        }
        val fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC)
        val toExclusive = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        val entries =
            weightEntryRepository.findAllByUser_IdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                userId,
                fromTs,
                toExclusive,
            )
        val mapped = entries.map { it.toResponse() }
        if (entries.isEmpty()) {
            return WeightStatsResponse(
                from = from,
                to = to,
                count = 0,
                minWeightKg = null,
                maxWeightKg = null,
                avgWeightKg = null,
                entries = mapped,
            )
        }
        val weights = entries.map { it.weightKg }
        val sum = weights.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
        val avg = sum.divide(BigDecimal.valueOf(weights.size.toLong()), 3, RoundingMode.HALF_UP)
        return WeightStatsResponse(
            from = from,
            to = to,
            count = entries.size,
            minWeightKg = weights.minOrNull(),
            maxWeightKg = weights.maxOrNull(),
            avgWeightKg = avg,
            entries = mapped,
        )
    }

    private fun findOwned(userId: UUID, entryId: UUID): WeightEntry =
        weightEntryRepository.findByIdAndUser_Id(entryId, userId)
            ?: throw WeightEntryNotFoundException(entryId)

    private fun syncProfileWeightFromLatest(userId: UUID) {
        val profile = userProfileRepository.findByIdOrNull(userId) ?: return
        val latest = weightEntryRepository.findTopByUser_IdOrderByMeasuredAtDesc(userId) ?: return
        profile.weightKg = latest.weightKg.toDouble()
        userProfileRepository.save(profile)
    }

    private fun WeightEntry.toResponse(): WeightEntryResponse =
        WeightEntryResponse(
            id = id,
            weightKg = weightKg,
            measuredAt = measuredAt,
            note = note,
            createdAt = requireNotNull(createdAt),
            updatedAt = requireNotNull(updatedAt),
        )
}
