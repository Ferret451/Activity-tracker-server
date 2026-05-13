package com.bsuir.Activity_tracker_server.food

import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient

/**
 * Parses USDA FDC nutrient lists into the same shape as public food API responses.
 */
object UsdaNutrientsParser {

    fun extractNutrients(nutrients: List<UsdaFoodNutrient>): FoodNutrientsResponse {
        val normalized = nutrients.mapNotNull(::toNutrientValue)

        val caloriesKcal = normalized.findValue(
            numberCodes = ENERGY_CODES,
            nameMatcher = { name ->
                name.contains("energy") || name.contains("calories") || name.contains("kcal")
            },
        )
        val proteinG = normalized.findValue(
            numberCodes = PROTEIN_CODES,
            nameMatcher = { name -> name.contains("protein") },
        )
        val fatG = normalized.findValue(
            numberCodes = FAT_CODES,
            nameMatcher = { name -> name.contains("fat") || name.contains("lipid") },
        )
        val carbsG = normalized.findValue(
            numberCodes = CARBS_CODES,
            nameMatcher = { name -> name.contains("carbohydrate") || name.contains("carbs") },
        )

        val additionalNutrients = normalized
            .asSequence()
            .filter { it.amount != 0.0 }
            .filterNot(::isEnergyOrMacro)
            .mapNotNull { value ->
                val name = value.name?.trim()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                FoodNutrientEntry(
                    name = name,
                    amount = value.amount,
                    unitName = value.unitName,
                    number = value.number,
                )
            }
            .toList()

        return FoodNutrientsResponse(
            caloriesKcal = caloriesKcal,
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG,
            additionalNutrients = additionalNutrients,
        )
    }

    fun hasRequiredMacros(nutrients: FoodNutrientsResponse): Boolean =
        nutrients.proteinG != null && nutrients.fatG != null && nutrients.carbsG != null

    /**
     * Search and list only include items with a known positive energy value (kcal per 100 g).
     * Missing or zero calories are excluded even when protein/fat/carbs are present.
     */
    fun caloriesAcceptableForSearchList(nutrients: FoodNutrientsResponse): Boolean {
        val kcal = nutrients.caloriesKcal ?: return false
        return kcal > 0.0
    }

    private fun toNutrientValue(nutrient: UsdaFoodNutrient): NutrientValue? {
        val amount = nutrient.amount ?: nutrient.value ?: return null
        val number = nutrient.nutrient?.number ?: nutrient.nutrientNumber ?: nutrient.number
        val name = nutrient.nutrient?.name ?: nutrient.nutrientName ?: nutrient.name
        val unitName = nutrient.nutrient?.unitName ?: nutrient.unitName

        return NutrientValue(
            number = number?.trim()?.lowercase(),
            name = name?.trim()?.lowercase(),
            amount = amount,
            unitName = unitName?.trim(),
        )
    }

    private fun List<NutrientValue>.findValue(
        numberCodes: Set<String>,
        nameMatcher: (String) -> Boolean,
    ): Double? {
        val byCode = firstOrNull { value ->
            value.number != null && value.number in numberCodes
        }
        if (byCode != null) {
            return byCode.amount
        }

        return firstOrNull { value ->
            value.name != null && nameMatcher(value.name)
        }?.amount
    }

    private data class NutrientValue(
        val number: String?,
        val name: String?,
        val amount: Double,
        val unitName: String?,
    )

    private fun isEnergyOrMacro(value: NutrientValue): Boolean {
        val number = value.number
        if (number != null && number in ENERGY_OR_MACRO_CODES) {
            return true
        }

        val name = value.name ?: return false
        return name.contains("energy") ||
            name.contains("calories") ||
            name.contains("kcal") ||
            name.contains("protein") ||
            name.contains("carbohydrate") ||
            name.contains("carbs") ||
            name.contains("fat") ||
            name.contains("lipid")
    }

    private val ENERGY_CODES = setOf("1008", "208")
    private val PROTEIN_CODES = setOf("1003", "203")
    private val FAT_CODES = setOf("1004", "204")
    private val CARBS_CODES = setOf("1005", "205")
    private val ENERGY_OR_MACRO_CODES = ENERGY_CODES + PROTEIN_CODES + FAT_CODES + CARBS_CODES
}
