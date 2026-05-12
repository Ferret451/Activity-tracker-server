package com.bsuir.Activity_tracker_server.nutrition

import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.bsuir.Activity_tracker_server.nutrition.exception.DuplicateFoodItemException
import com.bsuir.Activity_tracker_server.nutrition.exception.DuplicateMealTypeException
import com.bsuir.Activity_tracker_server.nutrition.exception.FoodItemInUseException
import com.bsuir.Activity_tracker_server.nutrition.exception.FoodItemNotFoundException
import com.bsuir.Activity_tracker_server.nutrition.exception.MealTypeInUseException
import com.bsuir.Activity_tracker_server.nutrition.exception.MealTypeNotFoundException
import com.bsuir.Activity_tracker_server.nutrition.exception.NutritionRecordNotFoundException
import com.bsuir.Activity_tracker_server.profile.ProfileService
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DateTimeException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

@Service
class NutritionService(
    private val userRepository: UserRepository,
    private val mealTypeRepository: MealTypeRepository,
    private val foodItemRepository: FoodItemRepository,
    private val nutritionRecordRepository: NutritionRecordRepository,
    private val foodDataClient: FoodDataClient,
    private val profileService: ProfileService,
) {

    @Transactional
    fun createMealType(request: CreateMealTypeRequest): MealTypeResponse {
        val normalizedName = request.name.trim()
        if (mealTypeRepository.existsByNameIgnoreCase(normalizedName)) {
            throw DuplicateMealTypeException(normalizedName)
        }

        val mealType = MealType(name = normalizedName)
        return mealTypeRepository.save(mealType).toResponse()
    }

    @Transactional(readOnly = true)
    fun getMealTypes(): List<MealTypeResponse> =
        mealTypeRepository.findAllByOrderByNameAsc()
            .map { it.toResponse() }

    @Transactional
    fun updateMealType(mealTypeId: UUID, request: UpdateMealTypeRequest): MealTypeResponse {
        val mealType = findMealTypeById(mealTypeId)
        val normalizedName = request.name.trim()

        if (!mealType.name.equals(normalizedName, ignoreCase = true) &&
            mealTypeRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, mealTypeId)
        ) {
            throw DuplicateMealTypeException(normalizedName)
        }

        mealType.name = normalizedName
        return mealTypeRepository.save(mealType).toResponse()
    }

    @Transactional
    fun deleteMealType(mealTypeId: UUID) {
        val mealType = findMealTypeById(mealTypeId)
        if (nutritionRecordRepository.existsByMealType_Id(mealTypeId)) {
            throw MealTypeInUseException(mealTypeId)
        }

        mealTypeRepository.delete(mealType)
    }

    @Transactional
    fun createFoodItem(userId: UUID, request: CreateFoodItemRequest): FoodItemResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val normalizedName = request.name.trim()
        if (foodItemRepository.existsByUser_IdAndNameIgnoreCase(userId, normalizedName)) {
            throw DuplicateFoodItemException(normalizedName)
        }

        val foodItem = FoodItem(
            user = user,
            name = normalizedName,
            source = request.source?.trim()?.takeIf(String::isNotBlank),
            fdcId = request.fdcId,
            caloriesPer100g = request.caloriesPer100g.toBd2(),
            proteinPer100g = request.proteinPer100g.toBd2(),
            fatPer100g = request.fatPer100g.toBd2(),
            carbsPer100g = request.carbsPer100g.toBd2(),
            note = request.note?.trim()?.takeIf(String::isNotBlank),
        )

        return foodItemRepository.save(foodItem).toResponse()
    }

    @Transactional
    fun importFoodItemFromUsda(userId: UUID, fdcId: Long): FoodItemResponse {
        require(fdcId > 0) { "Parameter 'fdcId' must be positive" }

        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val usdaFood = foodDataClient.getFoodById(fdcId)
        val nutrients = extractUsdaNutrients(usdaFood.foodNutrients)
        val baseName = usdaFood.description?.trim()?.takeIf(String::isNotBlank)
            ?: "USDA Food $fdcId"

        val existingFoodItem = foodItemRepository.findByUser_IdAndFdcId(userId, fdcId)
        if (existingFoodItem != null) {
            existingFoodItem.name = resolveImportedName(userId, baseName, fdcId, existingFoodItem.id)
            existingFoodItem.source = USDA_SOURCE
            existingFoodItem.caloriesPer100g = nutrients.calories.toBd2()
            existingFoodItem.proteinPer100g = nutrients.protein.toBd2()
            existingFoodItem.fatPer100g = nutrients.fat.toBd2()
            existingFoodItem.carbsPer100g = nutrients.carbs.toBd2()
            existingFoodItem.note = existingFoodItem.note ?: IMPORTED_NOTE
            return foodItemRepository.save(existingFoodItem).toResponse()
        }

        val importedFoodItem = FoodItem(
            user = user,
            name = resolveImportedName(userId, baseName, fdcId),
            source = USDA_SOURCE,
            fdcId = fdcId,
            caloriesPer100g = nutrients.calories.toBd2(),
            proteinPer100g = nutrients.protein.toBd2(),
            fatPer100g = nutrients.fat.toBd2(),
            carbsPer100g = nutrients.carbs.toBd2(),
            note = IMPORTED_NOTE,
        )

        return foodItemRepository.save(importedFoodItem).toResponse()
    }

    @Transactional(readOnly = true)
    fun getFoodItems(userId: UUID): List<FoodItemResponse> =
        foodItemRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getFoodItem(userId: UUID, foodItemId: UUID): FoodItemResponse =
        findOwnedFoodItem(userId, foodItemId).toResponse()

    @Transactional
    fun updateFoodItem(userId: UUID, foodItemId: UUID, request: UpdateFoodItemRequest): FoodItemResponse {
        val foodItem = findOwnedFoodItem(userId, foodItemId)
        val normalizedName = request.name.trim()

        if (!foodItem.name.equals(normalizedName, ignoreCase = true) &&
            foodItemRepository.existsByUser_IdAndNameIgnoreCase(userId, normalizedName)
        ) {
            throw DuplicateFoodItemException(normalizedName)
        }

        foodItem.name = normalizedName
        foodItem.source = request.source?.trim()?.takeIf(String::isNotBlank)
        foodItem.fdcId = request.fdcId
        foodItem.caloriesPer100g = request.caloriesPer100g.toBd2()
        foodItem.proteinPer100g = request.proteinPer100g.toBd2()
        foodItem.fatPer100g = request.fatPer100g.toBd2()
        foodItem.carbsPer100g = request.carbsPer100g.toBd2()
        foodItem.note = request.note?.trim()?.takeIf(String::isNotBlank)

        return foodItemRepository.save(foodItem).toResponse()
    }

    @Transactional
    fun deleteFoodItem(userId: UUID, foodItemId: UUID) {
        val foodItem = findOwnedFoodItem(userId, foodItemId)
        if (nutritionRecordRepository.existsByFoodItem_IdAndUser_Id(foodItemId, userId)) {
            throw FoodItemInUseException(foodItemId)
        }

        foodItemRepository.delete(foodItem)
    }

    @Transactional
    fun createRecord(userId: UUID, request: CreateNutritionRecordRequest): NutritionRecordResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)
        val mealType = findMealTypeById(request.mealTypeId)
        val foodItem = findOwnedFoodItem(userId, request.foodItemId)

        val grams = request.grams.toBd2()
        val totals = NutritionTotalsCalculator.calculate(foodItem, grams)

        val record = NutritionRecord(
            user = user,
            mealType = mealType,
            foodItem = foodItem,
            grams = grams,
            totalCaloriesKcal = totals.caloriesKcal,
            totalProteinG = totals.proteinG,
            totalFatG = totals.fatG,
            totalCarbsG = totals.carbsG,
            note = request.note?.trim()?.takeIf(String::isNotBlank),
            consumedAt = request.consumedAt ?: OffsetDateTime.now(ZoneOffset.UTC),
        )

        return nutritionRecordRepository.save(record).toResponse()
    }

    @Transactional(readOnly = true)
    fun getRecords(userId: UUID, filter: NutritionQueryFilter): List<NutritionRecordResponse> {
        val records = getFilteredRecords(userId, filter)
        return records.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getRecord(userId: UUID, recordId: UUID): NutritionRecordResponse =
        findOwnedRecord(userId, recordId).toResponse()

    @Transactional
    fun updateRecord(userId: UUID, recordId: UUID, request: UpdateNutritionRecordRequest): NutritionRecordResponse {
        val record = findOwnedRecord(userId, recordId)
        val mealType = findMealTypeById(request.mealTypeId)
        val foodItem = findOwnedFoodItem(userId, request.foodItemId)
        val grams = request.grams.toBd2()
        val totals = NutritionTotalsCalculator.calculate(foodItem, grams)

        record.mealType = mealType
        record.foodItem = foodItem
        record.grams = grams
        record.totalCaloriesKcal = totals.caloriesKcal
        record.totalProteinG = totals.proteinG
        record.totalFatG = totals.fatG
        record.totalCarbsG = totals.carbsG
        record.note = request.note?.trim()?.takeIf(String::isNotBlank)
        record.consumedAt = request.consumedAt

        return nutritionRecordRepository.save(record).toResponse()
    }

    @Transactional
    fun deleteRecord(userId: UUID, recordId: UUID) {
        val record = findOwnedRecord(userId, recordId)
        nutritionRecordRepository.delete(record)
    }

    @Transactional(readOnly = true)
    fun getHistory(userId: UUID, from: LocalDate?, to: LocalDate?): List<NutritionHistoryDayResponse> {
        val records = getFilteredRecords(
            userId = userId,
            filter = NutritionQueryFilter(from = from, to = to, mealTypeId = null, foodItemId = null),
        )

        return records
            .groupBy { it.consumedAt.toLocalDate() }
            .toList()
            .sortedByDescending { it.first }
            .map { (date, dayRecords) ->
                val totalCalories = dayRecords.fold(BigDecimal.ZERO) { acc, it -> acc + it.totalCaloriesKcal }
                val totalProtein = dayRecords.fold(BigDecimal.ZERO) { acc, it -> acc + it.totalProteinG }
                val totalFat = dayRecords.fold(BigDecimal.ZERO) { acc, it -> acc + it.totalFatG }
                val totalCarbs = dayRecords.fold(BigDecimal.ZERO) { acc, it -> acc + it.totalCarbsG }
                NutritionHistoryDayResponse(
                    date = date,
                    totalCaloriesKcal = roundToTwo(totalCalories.toDouble()),
                    totalProteinG = roundToTwo(totalProtein.toDouble()),
                    totalFatG = roundToTwo(totalFat.toDouble()),
                    totalCarbsG = roundToTwo(totalCarbs.toDouble()),
                    records = dayRecords.map { it.toResponse() },
                )
            }
    }

    @Transactional(readOnly = true)
    fun getSummary(userId: UUID, filter: NutritionQueryFilter): NutritionSummaryResponse {
        val records = getFilteredRecords(userId, filter)

        val items = records.map {
            NutritionSummaryItem(
                mealTypeId = it.mealType.id,
                mealTypeName = it.mealType.name,
                caloriesKcal = it.totalCaloriesKcal,
                proteinG = it.totalProteinG,
                fatG = it.totalFatG,
                carbsG = it.totalCarbsG,
            )
        }

        return NutritionSummaryCalculator.calculate(items)
    }

    @Transactional(readOnly = true)
    fun getDaily(userId: UUID, date: LocalDate?, zoneId: String): NutritionDailyResponse {
        val zone = resolveZoneId(zoneId)
        val day = date ?: LocalDate.now(zone)
        val start = day.atStartOfDay(zone).toOffsetDateTime()
        val endExclusive = day.plusDays(1).atStartOfDay(zone).toOffsetDateTime()

        val spec =
            Specification.where(NutritionRecordSpecifications.forUser(userId))
                .and(NutritionRecordSpecifications.consumedFrom(start))
                .and(NutritionRecordSpecifications.consumedUntil(endExclusive))

        val records = nutritionRecordRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "consumedAt"))

        val totalCalories = records.fold(BigDecimal.ZERO) { acc, it -> acc + it.totalCaloriesKcal }
        val totalProtein = records.fold(BigDecimal.ZERO) { acc, it -> acc + it.totalProteinG }
        val totalFat = records.fold(BigDecimal.ZERO) { acc, it -> acc + it.totalFatG }
        val totalCarbs = records.fold(BigDecimal.ZERO) { acc, it -> acc + it.totalCarbsG }

        val consumedKcal = roundToTwo(totalCalories.toDouble())
        val consumedProtein = roundToTwo(totalProtein.toDouble())
        val consumedFat = roundToTwo(totalFat.toDouble())
        val consumedCarbs = roundToTwo(totalCarbs.toDouble())

        val profile = profileService.getProfile(userId)
        val (targetKcal, source) =
            profile.dailyCalorieTargetKcal?.let { explicit ->
                explicit.toDouble() to NutritionDailyCalorieTargetSource.EXPLICIT_DAILY_TARGET
            } ?: run {
                profileService.calculateBmr(userId).goalAdjustedDailyCalories to
                    NutritionDailyCalorieTargetSource.BMR_GOAL_ADJUSTED
            }

        val remaining = roundToTwo(targetKcal - consumedKcal)

        return NutritionDailyResponse(
            date = day,
            zone = zone.id,
            totalCaloriesKcal = consumedKcal,
            totalProteinG = consumedProtein,
            totalFatG = consumedFat,
            totalCarbsG = consumedCarbs,
            targetCaloriesKcal = roundToTwo(targetKcal),
            calorieTargetSource = source,
            remainingCaloriesKcal = remaining,
        )
    }

    private fun resolveZoneId(zoneId: String): ZoneId {
        val trimmed = zoneId.trim()
        if (trimmed.isEmpty()) {
            return ZoneId.of("UTC")
        }
        return try {
            ZoneId.of(trimmed)
        } catch (e: DateTimeException) {
            throw IllegalArgumentException("Invalid zone id: '$trimmed'", e)
        }
    }

    private fun getFilteredRecords(userId: UUID, filter: NutritionQueryFilter): List<NutritionRecord> {
        validateDateRange(filter.from, filter.to)

        if (filter.mealTypeId != null) {
            findMealTypeById(filter.mealTypeId)
        }
        if (filter.foodItemId != null) {
            findOwnedFoodItem(userId, filter.foodItemId)
        }

        val fromDateTime = filter.from?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
        val toDateTimeExclusive = filter.to?.plusDays(1)?.atStartOfDay()?.atOffset(ZoneOffset.UTC)

        val spec =
            Specification.where(NutritionRecordSpecifications.forUser(userId))
                .and(NutritionRecordSpecifications.consumedFrom(fromDateTime))
                .and(NutritionRecordSpecifications.consumedUntil(toDateTimeExclusive))
                .and(NutritionRecordSpecifications.mealType(filter.mealTypeId))
                .and(NutritionRecordSpecifications.foodItem(filter.foodItemId))

        return nutritionRecordRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "consumedAt"))
    }

    private fun validateDateRange(from: LocalDate?, to: LocalDate?) {
        if (from != null && to != null && from.isAfter(to)) {
            throw IllegalArgumentException("Parameter 'from' must be before or equal to 'to'")
        }
    }

    private fun findMealTypeById(mealTypeId: UUID): MealType =
        mealTypeRepository.findByIdOrNull(mealTypeId)
            ?: throw MealTypeNotFoundException(mealTypeId)

    private fun findOwnedFoodItem(userId: UUID, foodItemId: UUID): FoodItem =
        foodItemRepository.findByIdAndUser_Id(foodItemId, userId)
            ?: throw FoodItemNotFoundException(foodItemId)

    private fun findOwnedRecord(userId: UUID, recordId: UUID): NutritionRecord =
        nutritionRecordRepository.findByIdAndUser_Id(recordId, userId)
            ?: throw NutritionRecordNotFoundException(recordId)

    private fun MealType.toResponse(): MealTypeResponse = MealTypeResponse(
        id = id,
        name = name,
        updatedAt = requireNotNull(updatedAt),
    )

    private fun FoodItem.toResponse(): FoodItemResponse = FoodItemResponse(
        id = id,
        name = name,
        caloriesPer100g = caloriesPer100g.toDouble(),
        proteinPer100g = proteinPer100g.toDouble(),
        fatPer100g = fatPer100g.toDouble(),
        carbsPer100g = carbsPer100g.toDouble(),
        source = source,
        fdcId = fdcId,
        note = note,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )

    private fun NutritionRecord.toResponse(): NutritionRecordResponse = NutritionRecordResponse(
        id = id,
        mealType = mealType.toResponse(),
        foodItem = foodItem.toResponse(),
        grams = grams.toDouble(),
        totalCaloriesKcal = totalCaloriesKcal.toDouble(),
        totalProteinG = totalProteinG.toDouble(),
        totalFatG = totalFatG.toDouble(),
        totalCarbsG = totalCarbsG.toDouble(),
        note = note,
        consumedAt = consumedAt,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )

    private fun roundToTwo(value: Double): Double = kotlin.math.round(value * 100) / 100

    private fun Double.toBd2(): BigDecimal =
        BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)

    private fun extractUsdaNutrients(nutrients: List<UsdaFoodNutrient>): UsdaNutrients {
        val normalized = nutrients.mapNotNull(::toNutrientValue)
        return UsdaNutrients(
            calories = roundToTwo(normalized.findValue(CALORIES_CODES) { name ->
                name.contains("energy") || name.contains("calories") || name.contains("kcal")
            } ?: 0.0),
            protein = roundToTwo(normalized.findValue(PROTEIN_CODES) { name -> name.contains("protein") } ?: 0.0),
            fat = roundToTwo(normalized.findValue(FAT_CODES) { name ->
                name.contains("fat") || name.contains("lipid")
            } ?: 0.0),
            carbs = roundToTwo(normalized.findValue(CARBS_CODES) { name ->
                name.contains("carbohydrate") || name.contains("carbs")
            } ?: 0.0),
        )
    }

    private fun toNutrientValue(nutrient: UsdaFoodNutrient): NutrientValue? {
        val amount = nutrient.amount ?: nutrient.value ?: return null
        val number = nutrient.nutrient?.number ?: nutrient.nutrientNumber ?: nutrient.number
        val name = nutrient.nutrient?.name ?: nutrient.nutrientName ?: nutrient.name

        return NutrientValue(
            number = number?.trim()?.lowercase(),
            name = name?.trim()?.lowercase(),
            amount = amount,
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

    private fun resolveImportedName(userId: UUID, baseName: String, fdcId: Long, currentFoodItemId: UUID? = null): String {
        val trimmed = baseName.trim().ifBlank { "USDA Food $fdcId" }
        if (currentFoodItemId != null) {
            val current = foodItemRepository.findByIdAndUser_Id(currentFoodItemId, userId)
            if (current != null && current.name.equals(trimmed, ignoreCase = true)) {
                return trimmed.take(MAX_FOOD_ITEM_NAME_LENGTH)
            }
        }

        if (!foodItemRepository.existsByUser_IdAndNameIgnoreCase(userId, trimmed)) {
            return trimmed.take(MAX_FOOD_ITEM_NAME_LENGTH)
        }

        val candidate = "$trimmed (FDC $fdcId)".take(MAX_FOOD_ITEM_NAME_LENGTH)
        if (!foodItemRepository.existsByUser_IdAndNameIgnoreCase(userId, candidate)) {
            return candidate
        }

        throw DuplicateFoodItemException(trimmed)
    }

    private data class NutrientValue(
        val number: String?,
        val name: String?,
        val amount: Double,
    )

    private data class UsdaNutrients(
        val calories: Double,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
    )

    companion object {
        private const val USDA_SOURCE = "USDA"
        private const val IMPORTED_NOTE = "Imported from USDA"
        private const val MAX_FOOD_ITEM_NAME_LENGTH = 120
        private val CALORIES_CODES = setOf("1008", "208")
        private val PROTEIN_CODES = setOf("1003", "203")
        private val FAT_CODES = setOf("1004", "204")
        private val CARBS_CODES = setOf("1005", "205")
    }
}

