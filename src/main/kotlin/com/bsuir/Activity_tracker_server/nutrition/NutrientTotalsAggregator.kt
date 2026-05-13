package com.bsuir.Activity_tracker_server.nutrition

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

private data class NutrientMergeKey(
    val numberNorm: String,
    val nameLower: String,
)

private data class MutableNutrientAgg(
    var number: String?,
    var name: String,
    var unitName: String?,
    var total: BigDecimal,
)

object NutrientTotalsAggregator {

    fun mergeAdditionalNutrients(rows: Iterable<NutritionRecordNutrient>): List<AdditionalNutrientTotalResponse> {
        val map = linkedMapOf<NutrientMergeKey, MutableNutrientAgg>()
        for (r in rows) {
            val name = r.name.trim()
            val key = NutrientMergeKey(
                numberNorm = r.nutrientNumber?.trim()?.lowercase().orEmpty(),
                nameLower = name.lowercase(),
            )
            val agg = map.getOrPut(key) {
                MutableNutrientAgg(
                    number = r.nutrientNumber?.trim()?.takeIf { it.isNotEmpty() },
                    name = name,
                    unitName = r.unitName?.trim()?.takeIf { it.isNotEmpty() },
                    total = BigDecimal.ZERO,
                )
            }
            agg.total = agg.total.add(r.totalAmount)
            if (agg.unitName == null) {
                r.unitName?.trim()?.takeIf { it.isNotEmpty() }?.let { agg.unitName = it }
            }
            if (agg.number == null) {
                r.nutrientNumber?.trim()?.takeIf { it.isNotEmpty() }?.let { agg.number = it }
            }
        }
        return map.values.map {
            AdditionalNutrientTotalResponse(
                number = it.number,
                name = it.name,
                unitName = it.unitName,
                totalAmount = roundToTwo(it.total),
            )
        }.sortedWith(compareBy({ it.name.lowercase() }, { it.number.orEmpty() }))
    }

    fun aggregateFromRecords(records: List<NutritionRecord>): AggregatedAdditionalNutrients {
        val overall = mergeAdditionalNutrients(records.flatMap { it.additionalNutrients })
        val byMealType = records
            .groupBy { it.mealType.id to it.mealType.name }
            .map { (key, recs) ->
                key.first to mergeAdditionalNutrients(recs.flatMap { it.additionalNutrients })
            }
            .toMap()
        return AggregatedAdditionalNutrients(overall = overall, byMealTypeId = byMealType)
    }

    private fun roundToTwo(value: BigDecimal): Double =
        value.setScale(2, RoundingMode.HALF_UP).toDouble()
}

data class AggregatedAdditionalNutrients(
    val overall: List<AdditionalNutrientTotalResponse>,
    val byMealTypeId: Map<UUID, List<AdditionalNutrientTotalResponse>>,
)
