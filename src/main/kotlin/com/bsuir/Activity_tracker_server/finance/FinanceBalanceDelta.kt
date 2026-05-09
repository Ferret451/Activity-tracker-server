package com.bsuir.Activity_tracker_server.finance

import java.math.BigDecimal

internal object FinanceBalanceDelta {
    fun signedContribution(operationType: FinanceOperationType, amount: BigDecimal): BigDecimal =
        when (operationType) {
            FinanceOperationType.INCOME -> amount
            FinanceOperationType.EXPENSE -> amount.negate()
        }
}
