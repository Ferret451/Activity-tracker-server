package com.bsuir.Activity_tracker_server.finance

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FinanceAccountRepository : JpaRepository<FinanceAccount, UUID> {
    fun findByIdAndUser_Id(id: UUID, userId: UUID): FinanceAccount?
    fun findAllByUser_IdOrderByIsPrimaryDescCreatedAtDesc(userId: UUID): List<FinanceAccount>
    fun findByUser_IdAndIsPrimaryIsTrue(userId: UUID): FinanceAccount?
    fun existsByUser_Id(userId: UUID): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE FinanceAccount a SET a.isPrimary = false
        WHERE a.user.id = :userId AND a.id <> :exceptId AND a.isPrimary = true
        """,
    )
    fun clearPrimaryFlagForUserExcept(@Param("userId") userId: UUID, @Param("exceptId") exceptId: UUID): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE FinanceAccount a SET a.isPrimary = false
        WHERE a.user.id = :userId AND a.isPrimary = true
        """,
    )
    fun clearAllPrimaryFlagsForUser(@Param("userId") userId: UUID): Int
}
