package com.bsuir.Activity_tracker_server.activity

import com.bsuir.Activity_tracker_server.activity.exception.ActivityRecordNotFoundException
import com.bsuir.Activity_tracker_server.activity.exception.ActivityTypeInUseException
import com.bsuir.Activity_tracker_server.activity.exception.ActivityTypeNotFoundException
import com.bsuir.Activity_tracker_server.activity.exception.DuplicateActivityTypeException
import com.bsuir.Activity_tracker_server.profile.UserProfileRepository
import com.bsuir.Activity_tracker_server.profile.exception.ProfileNotFoundException
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class ActivityService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val activityTypeRepository: ActivityTypeRepository,
    private val activityRecordRepository: ActivityRecordRepository,
) {

    @Transactional
    fun createType(request: CreateActivityTypeRequest): ActivityTypeResponse {
        val normalizedName = request.name.trim()
        if (activityTypeRepository.existsByNameIgnoreCaseAndMet(normalizedName, request.met)) {
            throw DuplicateActivityTypeException(normalizedName)
        }

        val activityType = ActivityType(
            name = normalizedName,
            met = request.met,
            supportsDistance = request.supportsDistance,
        )

        return activityTypeRepository.save(activityType).toResponse()
    }

    @Transactional(readOnly = true)
    fun getTypes(): List<ActivityTypeResponse> =
        activityTypeRepository.findAllByOrderByNameAsc()
            .map { it.toResponse() }

    @Transactional
    fun updateType(typeId: UUID, request: UpdateActivityTypeRequest): ActivityTypeResponse {
        val type = findTypeById(typeId)
        val normalizedName = request.name.trim()

        if (activityTypeRepository.existsByNameIgnoreCaseAndMetAndIdNot(normalizedName, request.met, typeId)) {
            throw DuplicateActivityTypeException(normalizedName)
        }

        type.name = normalizedName
        type.met = request.met
        type.supportsDistance = request.supportsDistance

        return activityTypeRepository.save(type).toResponse()
    }

    @Transactional
    fun deleteType(typeId: UUID) {
        val type = findTypeById(typeId)
        if (activityRecordRepository.existsByActivityType_Id(typeId)) {
            throw ActivityTypeInUseException(typeId)
        }

        activityTypeRepository.delete(type)
    }

    @Transactional
    fun createRecord(userId: UUID, request: CreateActivityRecordRequest): ActivityRecordResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)
        val profile = userProfileRepository.findByIdOrNull(userId)
            ?: throw ProfileNotFoundException(userId)
        val activityType = findTypeById(request.activityTypeId)
        activityType.requireSupportsDistance(request.distanceKm)

        val burnedCalories = ActivityCaloriesCalculator.calculate(
            weightKg = profile.weightKg,
            met = activityType.met,
            durationMinutes = request.durationMinutes,
        )

        val record = ActivityRecord(
            user = user,
            activityType = activityType,
            durationMinutes = request.durationMinutes,
            distanceKm = request.distanceKm,
            note = request.note?.trim()?.takeIf(String::isNotBlank),
            occurredAt = request.occurredAt ?: OffsetDateTime.now(ZoneOffset.UTC),
            burnedCaloriesKcal = burnedCalories,
        )

        return activityRecordRepository.save(record).toResponse()
    }

    @Transactional(readOnly = true)
    fun getRecords(userId: UUID, filter: ActivityQueryFilter): List<ActivityRecordResponse> {
        val records = getFilteredRecords(userId, filter)
        return records.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getRecord(userId: UUID, recordId: UUID): ActivityRecordResponse =
        findOwnedRecord(userId, recordId).toResponse()

    @Transactional
    fun updateRecord(userId: UUID, recordId: UUID, request: UpdateActivityRecordRequest): ActivityRecordResponse {
        val record = findOwnedRecord(userId, recordId)
        val profile = userProfileRepository.findByIdOrNull(userId)
            ?: throw ProfileNotFoundException(userId)
        val activityType = findTypeById(request.activityTypeId)
        activityType.requireSupportsDistance(request.distanceKm)

        record.activityType = activityType
        record.durationMinutes = request.durationMinutes
        record.distanceKm = request.distanceKm
        record.note = request.note?.trim()?.takeIf(String::isNotBlank)
        record.occurredAt = request.occurredAt
        record.burnedCaloriesKcal = ActivityCaloriesCalculator.calculate(
            weightKg = profile.weightKg,
            met = activityType.met,
            durationMinutes = request.durationMinutes,
        )

        return activityRecordRepository.save(record).toResponse()
    }

    @Transactional
    fun deleteRecord(userId: UUID, recordId: UUID) {
        val record = findOwnedRecord(userId, recordId)
        activityRecordRepository.delete(record)
    }

    @Transactional(readOnly = true)
    fun getSummary(userId: UUID, filter: ActivityQueryFilter): ActivitySummaryResponse {
        val records = getFilteredRecords(userId, filter)

        val items = records.map {
            ActivitySummaryItem(
                activityTypeId = it.activityType.id,
                activityTypeName = it.activityType.name,
                durationMinutes = it.durationMinutes,
                distanceKm = it.distanceKm ?: 0.0,
                burnedCaloriesKcal = it.burnedCaloriesKcal,
            )
        }

        return ActivitySummaryCalculator.calculate(items)
    }

    private fun getFilteredRecords(userId: UUID, filter: ActivityQueryFilter): List<ActivityRecord> {
        validateDateRange(filter.from, filter.to)

        if (filter.activityTypeId != null) {
            findTypeById(filter.activityTypeId)
        }

        val fromDateTime = filter.from?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
        val toDateTimeExclusive = filter.to?.plusDays(1)?.atStartOfDay()?.atOffset(ZoneOffset.UTC)

        return activityRecordRepository.findAllByUser_IdOrderByOccurredAtDesc(userId)
            .asSequence()
            .filter { fromDateTime == null || !it.occurredAt.isBefore(fromDateTime) }
            .filter { toDateTimeExclusive == null || it.occurredAt.isBefore(toDateTimeExclusive) }
            .filter { filter.activityTypeId == null || it.activityType.id == filter.activityTypeId }
            .toList()
    }

    private fun validateDateRange(from: LocalDate?, to: LocalDate?) {
        if (from != null && to != null && from.isAfter(to)) {
            throw IllegalArgumentException("Parameter 'from' must be before or equal to 'to'")
        }
    }

    private fun findTypeById(typeId: UUID): ActivityType =
        activityTypeRepository.findByIdOrNull(typeId)
            ?: throw ActivityTypeNotFoundException(typeId)

    private fun findOwnedRecord(userId: UUID, recordId: UUID): ActivityRecord =
        activityRecordRepository.findByIdAndUser_Id(recordId, userId)
            ?: throw ActivityRecordNotFoundException(recordId)

    private fun ActivityType.toResponse(): ActivityTypeResponse = ActivityTypeResponse(
        id = id,
        name = name,
        met = met,
        supportsDistance = supportsDistance,
        updatedAt = requireNotNull(updatedAt),
    )

    private fun ActivityRecord.toResponse(): ActivityRecordResponse = ActivityRecordResponse(
        id = id,
        activityType = activityType.toResponse(),
        durationMinutes = durationMinutes,
        distanceKm = distanceKm,
        note = note,
        occurredAt = occurredAt,
        burnedCaloriesKcal = burnedCaloriesKcal,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )
}
