package com.bsuir.Activity_tracker_server.activity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface WorkoutRoutePointRepository : JpaRepository<WorkoutRoutePoint, UUID> {
    fun findAllBySession_IdOrderByPointSeqAsc(sessionId: UUID): List<WorkoutRoutePoint>

    @Query("SELECT COALESCE(MAX(p.pointSeq), -1) FROM WorkoutRoutePoint p WHERE p.session.id = :sessionId")
    fun findMaxPointSeq(@Param("sessionId") sessionId: UUID): Int
}
