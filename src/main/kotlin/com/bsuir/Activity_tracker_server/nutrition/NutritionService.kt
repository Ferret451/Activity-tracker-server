package com.bsuir.Activity_tracker_server.nutrition

import com.bsuir.Activity_tracker_server.food.FoodNutrientEntry
import com.bsuir.Activity_tracker_server.food.UsdaNutrientsParser
import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.nutrition.exception.DuplicateFoodItemException
import com.bsuir.Activity_tracker_server.nutrition.exception.DuplicateMealTypeException
import com.bsuir.Activity_tracker_server.nutrition.exception.FoodItemInUseException
import com.bsuir.Activity_tracker_server.nutrition.exception.FoodItemNotFoundException
import com.bsuir.Activity_tracker_server.nutrition.exception.MealTypeInUseException
import com.bsuir.Activity_tracker_server.nutrition.exception.MealTypeNotFoundException
import com.bsuir.Activity_tracker_server.nutrition.exception.NutritionRecordNotFoundException
import com.bsuir.Activity_tracker_server.goal.GoalService
import com.bsuir.Activity_tracker_server.profile.ProfileService
import com.bsuir.Activity_tracker_server.user.User
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
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
    private val foodItemNutrientRepository: FoodItemNutrientRepository,
    private val nutritionRecordRepository: NutritionRecordRepository,
    private val nutritionRecordNutrientRepository: NutritionRecordNutrientRepository,
    private val foodDataClient: FoodDataClient,
    private val profileService: ProfileService,
    private val goalService: GoalService,
    private val transactionTemplate: TransactionTemplate,
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
        replaceAdditionalFromRequests(foodItem, request.additionalNutrients)

        return foodItemRepository.save(foodItem).toResponse()
    }

    /**
     * Оставлено для тестов и внутренних сценариев; публичный REST-вход — [logIntake].
     */
    fun importFoodItemFromUsda(userId: UUID, fdcId: Long): FoodItemResponse {
        require(fdcId > 0) { "Parameter 'fdcId' must be positive" }
        val usdaFood = foodDataClient.getFoodById(fdcId)
        return transactionTemplate.execute {
            persistFoodItemFromUsda(userId, fdcId, usdaFood)
        }!!.toResponse()
    }

    private fun persistFoodItemFromUsda(userId: UUID, fdcId: Long, usdaFood: UsdaFoodItemResponse): FoodItem {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val parsed = UsdaNutrientsParser.extractNutrients(usdaFood.foodNutrients)
        val baseName = usdaFood.description?.trim()?.takeIf(String::isNotBlank)
            ?: "USDA Food $fdcId"

        val existingFoodItem = foodItemRepository.findByUser_IdAndFdcId(userId, fdcId)
        if (existingFoodItem != null) {
            existingFoodItem.name = resolveImportedName(userId, baseName, fdcId, existingFoodItem.id)
            existingFoodItem.source = USDA_SOURCE
            existingFoodItem.caloriesPer100g = (parsed.caloriesKcal ?: 0.0).toBd2()
            existingFoodItem.proteinPer100g = (parsed.proteinG ?: 0.0).toBd2()
            existingFoodItem.fatPer100g = (parsed.fatG ?: 0.0).toBd2()
            existingFoodItem.carbsPer100g = (parsed.carbsG ?: 0.0).toBd2()
            existingFoodItem.note = existingFoodItem.note ?: IMPORTED_NOTE
            replaceAdditionalFromFoodEntries(existingFoodItem, parsed.additionalNutrients)
            return foodItemRepository.save(existingFoodItem)
        }

        val importedFoodItem = FoodItem(
            user = user,
            name = resolveImportedName(userId, baseName, fdcId),
            source = USDA_SOURCE,
            fdcId = fdcId,
            caloriesPer100g = (parsed.caloriesKcal ?: 0.0).toBd2(),
            proteinPer100g = (parsed.proteinG ?: 0.0).toBd2(),
            fatPer100g = (parsed.fatG ?: 0.0).toBd2(),
            carbsPer100g = (parsed.carbsG ?: 0.0).toBd2(),
            note = IMPORTED_NOTE,
        )
        replaceAdditionalFromFoodEntries(importedFoodItem, parsed.additionalNutrients)

        return foodItemRepository.save(importedFoodItem)
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
        replaceAdditionalFromRequests(foodItem, request.additionalNutrients)

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
        return saveNutritionRecord(
            user = user,
            mealType = mealType,
            foodItem = foodItem,
            grams = request.grams.toBd2(),
            note = request.note?.trim()?.takeIf(String::isNotBlank),
            consumedAt = request.consumedAt ?: OffsetDateTime.now(ZoneOffset.UTC),
        )
    }

    fun logIntake(userId: UUID, request: LogIntakeRequest): NutritionRecordResponse {
        val prefetchedUsdaFood = request.fdcId?.let { fdcId ->
            require(fdcId > 0) { "Parameter 'fdcId' must be positive" }
            foodDataClient.getFoodById(fdcId)
        }
        return transactionTemplate.execute {
            logIntakeInTransaction(userId, request, prefetchedUsdaFood)
        }!!
    }

    private fun logIntakeInTransaction(
        userId: UUID,
        request: LogIntakeRequest,
        prefetchedUsdaFood: UsdaFoodItemResponse?,
    ): NutritionRecordResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)
        val mealType = findMealTypeById(request.mealTypeId)
        val foodItem = when {
            request.fdcId != null -> persistFoodItemFromUsda(userId, request.fdcId, prefetchedUsdaFood!!)
            else -> findOwnedFoodItem(userId, request.foodItemId!!)
        }
        return saveNutritionRecord(
            user = user,
            mealType = mealType,
            foodItem = foodItem,
            grams = request.grams.toBd2(),
            note = request.note?.trim()?.takeIf(String::isNotBlank),
            consumedAt = request.consumedAt ?: OffsetDateTime.now(ZoneOffset.UTC),
        )
    }

    private fun saveNutritionRecord(
        user: User,
        mealType: MealType,
        foodItem: FoodItem,
        grams: BigDecimal,
        note: String?,
        consumedAt: OffsetDateTime,
    ): NutritionRecordResponse {
        val portionTotals = NutritionTotalsCalculator.calculatePortion(foodItem, grams)
        val record = NutritionRecord(
            user = user,
            mealType = mealType,
            foodItem = foodItem,
            grams = grams,
            totalCaloriesKcal = portionTotals.caloriesKcal,
            totalProteinG = portionTotals.proteinG,
            totalFatG = portionTotals.fatG,
            totalCarbsG = portionTotals.carbsG,
            note = note,
            consumedAt = consumedAt,
        )
        replaceAdditionalFromPortion(record, portionTotals.additionalNutrients)
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
        val portionTotals = NutritionTotalsCalculator.calculatePortion(foodItem, grams)

        record.mealType = mealType
        record.foodItem = foodItem
        record.grams = grams
        record.totalCaloriesKcal = portionTotals.caloriesKcal
        record.totalProteinG = portionTotals.proteinG
        record.totalFatG = portionTotals.fatG
        record.totalCarbsG = portionTotals.carbsG
        record.note = request.note?.trim()?.takeIf(String::isNotBlank)
        record.consumedAt = request.consumedAt
        replaceAdditionalFromPortion(record, portionTotals.additionalNutrients)

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
                val additionalDayTotals = NutrientTotalsAggregator.mergeAdditionalNutrients(
                    dayRecords.flatMap { it.additionalNutrients },
                )
                NutritionHistoryDayResponse(
                    date = date,
                    totalCaloriesKcal = roundToTwo(totalCalories.toDouble()),
                    totalProteinG = roundToTwo(totalProtein.toDouble()),
                    totalFatG = roundToTwo(totalFat.toDouble()),
                    totalCarbsG = roundToTwo(totalCarbs.toDouble()),
                    records = dayRecords.map { it.toResponse() },
                    additionalNutrientsTotals = additionalDayTotals,
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

        val macroSummary = NutritionSummaryCalculator.calculate(items)
        val agg = NutrientTotalsAggregator.aggregateFromRecords(records)
        return macroSummary.copy(
            additionalNutrientsTotals = agg.overall,
            byMealType = macroSummary.byMealType.map { m ->
                m.copy(additionalNutrientsTotals = agg.byMealTypeId[m.mealTypeId].orEmpty())
            },
        )
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
        val additionalDayTotals = NutrientTotalsAggregator.mergeAdditionalNutrients(
            records.flatMap { it.additionalNutrients },
        )

        profileService.getProfile(userId)
        val (targetKcal, source) =
            goalService.resolveActiveDailyCalorieTargetKcal(userId)?.let { target ->
                target to NutritionDailyCalorieTargetSource.USER_GOAL
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
            additionalNutrientsTotals = additionalDayTotals,
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

    private fun replaceAdditionalFromRequests(foodItem: FoodItem, requests: List<AdditionalNutrientRequest>?) {
        foodItemNutrientRepository.deleteByFoodItemId(foodItem.id)
        foodItem.additionalNutrients.clear()
        requests.orEmpty().forEach { req ->
            foodItem.additionalNutrients.add(
                FoodItemNutrient(
                    foodItem = foodItem,
                    nutrientNumber = req.number?.trim()?.takeIf { it.isNotEmpty() },
                    name = req.name.trim().take(200),
                    unitName = req.unitName?.trim()?.takeIf { it.isNotEmpty() },
                    amountPer100g = req.amountPer100g.toBd2(),
                )
            )
        }
    }

    private fun replaceAdditionalFromFoodEntries(foodItem: FoodItem, entries: List<FoodNutrientEntry>) {
        foodItemNutrientRepository.deleteByFoodItemId(foodItem.id)
        foodItem.additionalNutrients.clear()
        entries.forEach { e ->
            foodItem.additionalNutrients.add(
                FoodItemNutrient(
                    foodItem = foodItem,
                    nutrientNumber = e.number?.trim()?.takeIf { it.isNotEmpty() },
                    name = e.name.trim().take(200),
                    unitName = e.unitName?.trim()?.takeIf { it.isNotEmpty() },
                    amountPer100g = roundDoubleToBd2(e.amount),
                )
            )
        }
    }

    private fun replaceAdditionalFromPortion(record: NutritionRecord, totals: List<PortionNutrientTotal>) {
        nutritionRecordNutrientRepository.deleteByNutritionRecordId(record.id)
        record.additionalNutrients.clear()
        totals.forEach { p ->
            record.additionalNutrients.add(
                NutritionRecordNutrient(
                    nutritionRecord = record,
                    nutrientNumber = p.nutrientNumber,
                    name = p.name.trim().take(200),
                    unitName = p.unitName?.trim()?.takeIf { it.isNotEmpty() },
                    totalAmount = p.totalAmount,
                )
            )
        }
    }

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
        additionalNutrientsPer100g = additionalNutrients.map {
            AdditionalNutrientPer100gResponse(
                number = it.nutrientNumber,
                name = it.name,
                unitName = it.unitName,
                amountPer100g = it.amountPer100g.toDouble(),
            )
        }.sortedWith(compareBy({ it.name.lowercase() }, { it.number.orEmpty() })),
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
        additionalNutrients = additionalNutrients.map {
            AdditionalNutrientPortionResponse(
                number = it.nutrientNumber,
                name = it.name,
                unitName = it.unitName,
                totalAmount = it.totalAmount.toDouble(),
            )
        }.sortedWith(compareBy({ it.name.lowercase() }, { it.number.orEmpty() })),
    )

    private fun roundToTwo(value: Double): Double = kotlin.math.round(value * 100) / 100

    private fun Double.toBd2(): BigDecimal =
        BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)

    private fun roundDoubleToBd2(value: Double): BigDecimal =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)

    private fun resolveImportedName(userId: UUID, baseName: String, fdcId: Long, currentFoodItemId: UUID? = null): String {
        val trimmed = baseName.trim().ifBlank { "USDA Food $fdcId" }
        if (currentFoodItemId != null) {
            val current = foodItemRepository.findByIdAndUser_Id(currentFoodItemId, userId)
            if (current != null && current.name.equals(trimmed, ignoreCase = true)) {
                return trimmed.take(MAX_FOOD_ITEM_NAME_LENGTH)
            }
        }

        if (!nameTakenByAnotherUserFood(userId, trimmed, currentFoodItemId)) {
            return trimmed.take(MAX_FOOD_ITEM_NAME_LENGTH)
        }

        val candidate = "$trimmed (FDC $fdcId)".take(MAX_FOOD_ITEM_NAME_LENGTH)
        if (!nameTakenByAnotherUserFood(userId, candidate, currentFoodItemId)) {
            return candidate
        }

        throw DuplicateFoodItemException(trimmed)
    }

    private fun nameTakenByAnotherUserFood(userId: UUID, name: String, excludeFoodItemId: UUID?): Boolean =
        if (excludeFoodItemId != null) {
            foodItemRepository.existsByUser_IdAndNameIgnoreCaseAndIdNot(userId, name, excludeFoodItemId)
        } else {
            foodItemRepository.existsByUser_IdAndNameIgnoreCase(userId, name)
        }

    companion object {
        private const val USDA_SOURCE = "USDA"
        private const val IMPORTED_NOTE = "Imported from USDA"
        private const val MAX_FOOD_ITEM_NAME_LENGTH = 120
    }
}

