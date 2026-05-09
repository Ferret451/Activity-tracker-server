package com.bsuir.Activity_tracker_server.finance

import org.springframework.data.jpa.domain.Specification
import java.time.OffsetDateTime
import java.util.UUID

internal object FinanceRecordSpecifications {

    fun forUser(userId: UUID): Specification<FinanceRecord> =
        Specification { root, _, cb ->
            cb.equal(root.get<Any>("user").get<UUID>("id"), userId)
        }

    fun withBranch(branch: FinanceOperationType?): Specification<FinanceRecord> =
        Specification { root, _, cb ->
            if (branch == null) {
                cb.conjunction()
            } else {
                cb.equal(root.get<FinanceOperationType>("operationType"), branch)
            }
        }

    fun occurredFrom(fromInclusive: OffsetDateTime?): Specification<FinanceRecord> =
        Specification { root, _, cb ->
            if (fromInclusive == null) {
                cb.conjunction()
            } else {
                cb.greaterThanOrEqualTo(root.get("occurredAt"), fromInclusive)
            }
        }

    fun occurredUntil(toExclusive: OffsetDateTime?): Specification<FinanceRecord> =
        Specification { root, _, cb ->
            if (toExclusive == null) {
                cb.conjunction()
            } else {
                cb.lessThan(root.get("occurredAt"), toExclusive)
            }
        }

    fun category(categoryId: UUID?): Specification<FinanceRecord> =
        Specification { root, _, cb ->
            if (categoryId == null) {
                cb.conjunction()
            } else {
                cb.equal(root.get<FinanceCategory>("category").get<UUID>("id"), categoryId)
            }
        }

    fun account(accountId: UUID?): Specification<FinanceRecord> =
        Specification { root, _, cb ->
            if (accountId == null) {
                cb.conjunction()
            } else {
                cb.equal(root.get<FinanceAccount>("account").get<UUID>("id"), accountId)
            }
        }
}
