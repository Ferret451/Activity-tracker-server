package com.bsuir.Activity_tracker_server.nutrition

import org.springframework.data.jpa.domain.Specification
import java.time.OffsetDateTime
import java.util.UUID

internal object NutritionRecordSpecifications {

    fun forUser(userId: UUID): Specification<NutritionRecord> =
        Specification { root, _, cb ->
            cb.equal(root.get<Any>("user").get<UUID>("id"), userId)
        }

    fun consumedFrom(fromInclusive: OffsetDateTime?): Specification<NutritionRecord> =
        Specification { root, _, cb ->
            if (fromInclusive == null) {
                cb.conjunction()
            } else {
                cb.greaterThanOrEqualTo(root.get("consumedAt"), fromInclusive)
            }
        }

    fun consumedUntil(toExclusive: OffsetDateTime?): Specification<NutritionRecord> =
        Specification { root, _, cb ->
            if (toExclusive == null) {
                cb.conjunction()
            } else {
                cb.lessThan(root.get("consumedAt"), toExclusive)
            }
        }

    fun mealType(mealTypeId: UUID?): Specification<NutritionRecord> =
        Specification { root, _, cb ->
            if (mealTypeId == null) {
                cb.conjunction()
            } else {
                cb.equal(root.get<MealType>("mealType").get<UUID>("id"), mealTypeId)
            }
        }

    fun foodItem(foodItemId: UUID?): Specification<NutritionRecord> =
        Specification { root, _, cb ->
            if (foodItemId == null) {
                cb.conjunction()
            } else {
                cb.equal(root.get<FoodItem>("foodItem").get<UUID>("id"), foodItemId)
            }
        }
}
