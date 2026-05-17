package com.bsuir.Activity_tracker_server.activity

import com.bsuir.Activity_tracker_server.activity.exception.ActiveWorkoutAlreadyExistsException
import com.bsuir.Activity_tracker_server.activity.exception.ActivityTypeNotFoundException
import com.bsuir.Activity_tracker_server.activity.exception.InvalidWorkoutSessionStateException
import com.bsuir.Activity_tracker_server.activity.exception.WorkoutSessionNotFoundException
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.max

@Service
class WorkoutService(
    private val userRepository: UserRepository,
    private val activityTypeRepository: ActivityTypeRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutRoutePointRepository: WorkoutRoutePointRepository,
    private val activityRecordRepository: ActivityRecordRepository,
    private val activityService: ActivityService,
) {

    @Transactional
    fun startWorkout(userId: UUID, request: StartWorkoutRequest): WorkoutSessionResponse {
        if (workoutSessionRepository.existsByUser_IdAndStatus(userId, WorkoutSessionStatus.ACTIVE)) {
            throw ActiveWorkoutAlreadyExistsException()
        }
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val activityType =
            activityTypeRepository.findByIdOrNull(request.activityTypeId)
                ?: throw ActivityTypeNotFoundException(request.activityTypeId)

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val session =
            WorkoutSession(
                user = user,
                activityType = activityType,
                status = WorkoutSessionStatus.ACTIVE,
                startedAt = now,
            )
        return workoutSessionRepository.save(session).toResponse()
    }

    @Transactional(readOnly = true)
    fun listSessions(userId: UUID): List<WorkoutSessionResponse> =
        workoutSessionRepository.findAllByUser_IdOrderByStartedAtDesc(userId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getSession(userId: UUID, sessionId: UUID): WorkoutSessionResponse =
        findOwnedSession(userId, sessionId).toResponse()

    @Transactional
    fun appendRoutePoints(userId: UUID, sessionId: UUID, request: AppendWorkoutRoutePointsRequest) {
        val session = findOwnedSession(userId, sessionId)
        if (session.status != WorkoutSessionStatus.ACTIVE) {
            throw InvalidWorkoutSessionStateException("Route points can only be added to an active session")
        }
        session.activityType.requireSupportsDistanceForRoute()
        require(request.points.size <= 500) { "At most 500 points per request" }
        var nextSeq = workoutRoutePointRepository.findMaxPointSeq(sessionId) + 1
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val entities =
            request.points.map { p ->
                WorkoutRoutePoint(
                    session = session,
                    pointSeq = nextSeq++,
                    latitude = p.latitude,
                    longitude = p.longitude,
                    recordedAt = p.recordedAt ?: now,
                )
            }
        workoutRoutePointRepository.saveAll(entities)
    }

    @Transactional(readOnly = true)
    fun getRoute(userId: UUID, sessionId: UUID): List<WorkoutRoutePointResponse> {
        findOwnedSession(userId, sessionId)
        return workoutRoutePointRepository.findAllBySession_IdOrderByPointSeqAsc(sessionId)
            .map {
                WorkoutRoutePointResponse(
                    id = it.id,
                    pointSeq = it.pointSeq,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    recordedAt = it.recordedAt,
                )
            }
    }

    @Transactional
    fun stopWorkout(userId: UUID, sessionId: UUID, request: StopWorkoutRequest?): WorkoutSessionResponse {
        val session = findOwnedSession(userId, sessionId)
        if (session.status != WorkoutSessionStatus.ACTIVE) {
            throw InvalidWorkoutSessionStateException("Session is not active")
        }
        session.activityType.requireSupportsDistance(request?.distanceKm)

        val endedAt = request?.endedAt ?: OffsetDateTime.now(ZoneOffset.UTC)
        var durationMinutes = ChronoUnit.MINUTES.between(session.startedAt, endedAt).toInt()
        durationMinutes = max(1, durationMinutes)

        val recordResponse =
            activityService.createRecord(
                userId,
                CreateActivityRecordRequest(
                    activityTypeId = session.activityType.id,
                    durationMinutes = durationMinutes,
                    distanceKm = request?.distanceKm,
                    note = request?.note?.trim()?.takeIf(String::isNotBlank) ?: session.note,
                    occurredAt = endedAt,
                ),
            )

        val record =
            activityRecordRepository.findByIdAndUser_Id(recordResponse.id, userId)
                ?: error("Activity record was not found after creation")

        session.status = WorkoutSessionStatus.COMPLETED
        session.endedAt = endedAt
        session.activityRecord = record
        return workoutSessionRepository.save(session).toResponse()
    }

    @Transactional
    fun cancelWorkout(userId: UUID, sessionId: UUID): WorkoutSessionResponse {
        val session = findOwnedSession(userId, sessionId)
        if (session.status != WorkoutSessionStatus.ACTIVE) {
            throw InvalidWorkoutSessionStateException("Session is not active")
        }
        session.status = WorkoutSessionStatus.CANCELLED
        session.endedAt = OffsetDateTime.now(ZoneOffset.UTC)
        return workoutSessionRepository.save(session).toResponse()
    }

    private fun findOwnedSession(userId: UUID, sessionId: UUID): WorkoutSession =
        workoutSessionRepository.findByIdAndUser_Id(sessionId, userId)
            ?: throw WorkoutSessionNotFoundException(sessionId)

    private fun WorkoutSession.toResponse(): WorkoutSessionResponse =
        WorkoutSessionResponse(
            id = id,
            activityType = activityType.toResponse(),
            status = status,
            startedAt = startedAt,
            endedAt = endedAt,
            activityRecordId = activityRecord?.id,
            note = note,
            createdAt = requireNotNull(createdAt),
            updatedAt = requireNotNull(updatedAt),
        )

    private fun ActivityType.toResponse(): ActivityTypeResponse =
        ActivityTypeResponse(
            id = id,
            name = name,
            met = met,
            supportsDistance = supportsDistance,
            updatedAt = requireNotNull(updatedAt),
        )
}
