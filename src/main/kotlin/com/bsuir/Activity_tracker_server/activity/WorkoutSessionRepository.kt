package com.bsuir.Activity_tracker_server.activity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface WorkoutSessionRepository : JpaRepository<WorkoutSession, UUID> {
    fun existsByUser_IdAndStatus(userId: UUID, status: WorkoutSessionStatus): Boolean

    fun findAllByUser_IdOrderByStartedAtDesc(userId: UUID): List<WorkoutSession>

    @Query(
        """
        SELECT s FROM WorkoutSession s
        WHERE s.user.id = :userId AND s.id = :sessionId
        """,
    )
    fun findByIdAndUser_Id(
        @Param("sessionId") sessionId: UUID,
        @Param("userId") userId: UUID,
    ): WorkoutSession?

    fun findFirstByUser_IdAndStatusOrderByStartedAtDesc(
        userId: UUID,
        status: WorkoutSessionStatus,
    ): WorkoutSession?
}
