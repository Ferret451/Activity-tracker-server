package com.bsuir.Activity_tracker_server.demo

import com.bsuir.Activity_tracker_server.activity.ActivityService
import com.bsuir.Activity_tracker_server.activity.AppendWorkoutRoutePointsRequest
import com.bsuir.Activity_tracker_server.activity.CreateActivityRecordRequest
import com.bsuir.Activity_tracker_server.activity.RoutePointInput
import com.bsuir.Activity_tracker_server.activity.StartWorkoutRequest
import com.bsuir.Activity_tracker_server.activity.StopWorkoutRequest
import com.bsuir.Activity_tracker_server.activity.WorkoutService
import com.bsuir.Activity_tracker_server.auth.AuthService
import com.bsuir.Activity_tracker_server.auth.RegisterRequest
import com.bsuir.Activity_tracker_server.bodymetrics.CreateWeightEntryRequest
import com.bsuir.Activity_tracker_server.bodymetrics.StepService
import com.bsuir.Activity_tracker_server.bodymetrics.UpsertDailyStepsRequest
import com.bsuir.Activity_tracker_server.bodymetrics.WeightService
import com.bsuir.Activity_tracker_server.finance.CreateFinanceCategoryBranchRequest
import com.bsuir.Activity_tracker_server.finance.CreateFinanceRecordRequest
import com.bsuir.Activity_tracker_server.finance.FinanceOperationType
import com.bsuir.Activity_tracker_server.finance.FinanceService
import com.bsuir.Activity_tracker_server.goal.AddGoalProgressRequest
import com.bsuir.Activity_tracker_server.goal.CreateGoalRequest
import com.bsuir.Activity_tracker_server.goal.GoalService
import com.bsuir.Activity_tracker_server.goal.GoalStatus
import com.bsuir.Activity_tracker_server.goal.GoalType
import com.bsuir.Activity_tracker_server.goal.UpdateGoalStatusRequest
import com.bsuir.Activity_tracker_server.nutrition.CreateFoodItemRequest
import com.bsuir.Activity_tracker_server.nutrition.CreateNutritionRecordRequest
import com.bsuir.Activity_tracker_server.nutrition.MealTypeResponse
import com.bsuir.Activity_tracker_server.nutrition.NutritionService
import com.bsuir.Activity_tracker_server.activity.ActivityTypeResponse
import com.bsuir.Activity_tracker_server.profile.ActivityLevel
import com.bsuir.Activity_tracker_server.profile.CalorieGoal
import com.bsuir.Activity_tracker_server.profile.ProfileService
import com.bsuir.Activity_tracker_server.profile.Sex
import com.bsuir.Activity_tracker_server.profile.UpsertProfileRequest
import com.bsuir.Activity_tracker_server.settings.UserSettingsService
import com.bsuir.Activity_tracker_server.settings.UserSettingsUpdateRequest
import com.bsuir.Activity_tracker_server.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["app.demo.seed.enabled"], havingValue = "true")
class DemoDataSeeder(
    private val props: DemoDataProperties,
    private val userRepository: UserRepository,
    private val authService: AuthService,
    private val profileService: ProfileService,
    private val userSettingsService: UserSettingsService,
    private val nutritionService: NutritionService,
    private val activityService: ActivityService,
    private val workoutService: WorkoutService,
    private val financeService: FinanceService,
    private val goalService: GoalService,
    private val weightService: WeightService,
    private val stepService: StepService,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        val email = props.email.trim()
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Demo seed skipped: user with email '{}' already exists", email)
            return
        }

        val auth = authService.register(
            RegisterRequest(
                email = email,
                username = props.username.trim(),
                firstName = props.firstName.trim(),
                lastName = props.lastName.trim(),
                password = props.password,
            ),
        )
        val userId = auth.user.id

        seedProfile(userId)
        seedSettings(userId)

        val mealTypes = nutritionService.getMealTypes().associateBy { it.name.lowercase() }
        val activityTypes = activityService.getTypes().associateBy { it.name }

        val foodItems = seedFoodItems(userId)
        val financeCategories = seedFinanceCategories(userId)
        val goals = seedGoals(userId)

        val today = LocalDate.now(ZoneOffset.UTC)
        val historyDays = props.historyDays.coerceAtLeast(1)
        val startDate = today.minusDays((historyDays - 1).toLong())

        for (dayOffset in 0 until historyDays) {
            val date = startDate.plusDays(dayOffset.toLong())
            val dayIndex = dayOffset

            seedWeightEntry(userId, date, dayIndex, historyDays)
            seedSteps(userId, date, dayIndex)
            seedNutritionForDay(userId, date, mealTypes, foodItems, dayIndex)
            seedActivityForDay(userId, date, activityTypes, dayIndex)
            seedFinanceForDay(userId, date, financeCategories, dayIndex)

            val progressWeight = 80.0 - (dayIndex * 0.7)
            goalService.addProgress(
                userId,
                goals.weightGoalId,
                AddGoalProgressRequest(value = progressWeight, note = "День ${dayIndex + 1}"),
            )
        }

        seedWorkout(userId, today, activityTypes)

        log.info(
            """
            |
            |=== DEMO USER CREATED ===
            |Email:    {}
            |Password: {}
            |Username: {}
            |=========================
            """.trimMargin(),
            email,
            props.password,
            props.username.trim(),
        )
    }

    private fun seedProfile(userId: UUID) {
        profileService.upsertProfile(
            userId,
            UpsertProfileRequest(
                weightKg = 72.0,
                heightCm = 178,
                age = 28,
                sex = Sex.MALE,
                activityLevel = ActivityLevel.MODERATE,
                calorieGoal = CalorieGoal.WEIGHT_LOSS,
            ),
        )
    }

    private fun seedSettings(userId: UUID) {
        userSettingsService.putSettings(
            userId,
            UserSettingsUpdateRequest(
                preferences = mapOf(
                    "language" to "ru",
                    "theme" to "system",
                ),
            ),
        )
    }

    private data class FoodItemIds(
        val oats: UUID,
        val chicken: UUID,
        val apple: UUID,
        val rice: UUID,
    )

    private fun seedFoodItems(userId: UUID): FoodItemIds {
        fun item(name: String, cal: Double, protein: Double, fat: Double, carbs: Double): UUID =
            nutritionService.createFoodItem(
                userId,
                CreateFoodItemRequest(
                    name = name,
                    caloriesPer100g = cal,
                    proteinPer100g = protein,
                    fatPer100g = fat,
                    carbsPer100g = carbs,
                    source = "manual",
                ),
            ).id

        return FoodItemIds(
            oats = item("Овсянка", 380.0, 13.0, 7.0, 67.0),
            chicken = item("Куриная грудка", 165.0, 31.0, 3.6, 0.0),
            apple = item("Яблоко", 52.0, 0.3, 0.2, 13.8),
            rice = item("Рис отварной", 130.0, 2.7, 0.3, 28.0),
        )
    }

    private data class FinanceCategoryIds(
        val income: UUID,
        val food: UUID,
        val transport: UUID,
    )

    private fun seedFinanceCategories(userId: UUID): FinanceCategoryIds {
        fun category(branch: FinanceOperationType, name: String): UUID =
            financeService.createCategoryForBranch(
                userId,
                branch,
                CreateFinanceCategoryBranchRequest(name = name),
            ).id

        return FinanceCategoryIds(
            income = category(FinanceOperationType.INCOME, "Зарплата"),
            food = category(FinanceOperationType.EXPENSE, "Еда"),
            transport = category(FinanceOperationType.EXPENSE, "Транспорт"),
        )
    }

    private data class GoalIds(
        val weightGoalId: UUID,
    )

    private fun seedGoals(userId: UUID): GoalIds {
        val today = LocalDate.now(ZoneOffset.UTC)
        val weightGoal = goalService.createGoal(
            userId,
            CreateGoalRequest(
                goalType = GoalType.WEIGHT_KG,
                title = "Снизить вес до 75 кг",
                initialValue = 80.0,
                targetValue = 75.0,
                startDate = today.minusDays((props.historyDays - 1).toLong()),
                targetDate = today.plusDays(21),
            ),
        )

        goalService.createGoal(
            userId,
            CreateGoalRequest(
                goalType = GoalType.DAILY_STEPS,
                title = "10 000 шагов в день",
                initialValue = 0.0,
                targetValue = 10_000.0,
                startDate = today,
                targetDate = today.plusDays(30),
            ),
        )

        val archivedGoal = goalService.createGoal(
            userId,
            CreateGoalRequest(
                goalType = GoalType.DAILY_WALKING_MINUTES,
                title = "Ходить 60 минут в день",
                initialValue = 0.0,
                targetValue = 60.0,
                startDate = today.minusDays(14),
                targetDate = today,
            ),
        )
        goalService.updateStatus(
            userId,
            archivedGoal.id,
            UpdateGoalStatusRequest(status = GoalStatus.ARCHIVED),
        )

        return GoalIds(weightGoalId = weightGoal.id)
    }

    private fun seedWeightEntry(userId: UUID, date: LocalDate, dayIndex: Int, totalDays: Int) {
        val startWeight = 79.8
        val endWeight = 72.0
        val progress = if (totalDays <= 1) 1.0 else dayIndex.toDouble() / (totalDays - 1)
        val weightKg = startWeight + (endWeight - startWeight) * progress
        weightService.createEntry(
            userId,
            CreateWeightEntryRequest(
                weightKg = BigDecimal.valueOf(weightKg).setScale(1, java.math.RoundingMode.HALF_UP),
                measuredAt = date.atTimeUtc(8, 0),
                note = "Утреннее взвешивание",
            ),
        )
    }

    private fun seedSteps(userId: UUID, date: LocalDate, dayIndex: Int) {
        val steps = 6_000 + dayIndex * 900 + (dayIndex % 3) * 200
        val walkingMinutes = 45 + dayIndex * 5
        val burnedCalories = steps * 0.038
        stepService.upsertSteps(
            userId,
            date,
            UpsertDailyStepsRequest(
                steps = steps,
                burnedCaloriesKcal = burnedCalories,
                walkingDurationMinutes = walkingMinutes,
            ),
        )
    }

    private fun seedNutritionForDay(
        userId: UUID,
        date: LocalDate,
        mealTypes: Map<String, MealTypeResponse>,
        foodItems: FoodItemIds,
        dayIndex: Int,
    ) {
        val breakfast = mealTypes["breakfast"]?.id ?: return
        val lunch = mealTypes["lunch"]?.id ?: return
        val dinner = mealTypes["dinner"]?.id ?: return
        val snack = mealTypes["snack"]?.id

        fun record(mealTypeId: UUID, foodItemId: UUID, grams: Double, hour: Int) {
            nutritionService.createRecord(
                userId,
                CreateNutritionRecordRequest(
                    mealTypeId = mealTypeId,
                    foodItemId = foodItemId,
                    grams = grams,
                    consumedAt = date.atTimeUtc(hour, 0),
                ),
            )
        }

        record(breakfast, foodItems.oats, 60.0 + dayIndex * 5, 8)
        record(lunch, foodItems.chicken, 150.0, 13)
        record(dinner, foodItems.rice, 120.0 + dayIndex * 3, 19)
        if (snack != null && dayIndex % 2 == 0) {
            record(snack, foodItems.apple, 180.0, 16)
        }
    }

    private fun seedActivityForDay(
        userId: UUID,
        date: LocalDate,
        activityTypes: Map<String, ActivityTypeResponse>,
        dayIndex: Int,
    ) {
        val walking = activityTypes["Walking"] ?: return
        val yoga = activityTypes["Yoga"]

        if (dayIndex % 2 == 0) {
            activityService.createRecord(
                userId,
                CreateActivityRecordRequest(
                    activityTypeId = walking.id,
                    durationMinutes = 25 + dayIndex * 3,
                    distanceKm = 3.0 + dayIndex * 0.4,
                    note = "Прогулка",
                    occurredAt = date.atTimeUtc(10, 30),
                ),
            )
        }

        if (yoga != null && dayIndex % 2 == 1) {
            activityService.createRecord(
                userId,
                CreateActivityRecordRequest(
                    activityTypeId = yoga.id,
                    durationMinutes = 40,
                    note = "Йога",
                    occurredAt = date.atTimeUtc(18, 0),
                ),
            )
        }
    }

    private fun seedFinanceForDay(
        userId: UUID,
        date: LocalDate,
        categories: FinanceCategoryIds,
        dayIndex: Int,
    ) {
        if (dayIndex == 0) {
            financeService.createRecordForBranch(
                userId,
                FinanceOperationType.INCOME,
                CreateFinanceRecordRequest(
                    categoryId = categories.income,
                    amount = BigDecimal("3500.00"),
                    note = "Зарплата за месяц",
                    occurredAt = date.atTimeUtc(9, 0),
                ),
            )
        }

        if (dayIndex % 2 == 0) {
            financeService.createRecordForBranch(
                userId,
                FinanceOperationType.EXPENSE,
                CreateFinanceRecordRequest(
                    categoryId = categories.food,
                    amount = BigDecimal("45.50").add(BigDecimal.valueOf(dayIndex * 5L)),
                    note = "Продукты",
                    occurredAt = date.atTimeUtc(14, 0),
                ),
            )
        }

        if (dayIndex % 3 == 1) {
            financeService.createRecordForBranch(
                userId,
                FinanceOperationType.EXPENSE,
                CreateFinanceRecordRequest(
                    categoryId = categories.transport,
                    amount = BigDecimal("12.00").add(BigDecimal.valueOf(dayIndex * 2L)),
                    note = "Проезд",
                    occurredAt = date.atTimeUtc(17, 30),
                ),
            )
        }
    }

    private fun seedWorkout(
        userId: UUID,
        date: LocalDate,
        activityTypes: Map<String, ActivityTypeResponse>,
    ) {
        val jogging = activityTypes["Jogging"] ?: return
        val session = workoutService.startWorkout(
            userId,
            StartWorkoutRequest(activityTypeId = jogging.id),
        )
        workoutService.appendRoutePoints(
            userId,
            session.id,
            AppendWorkoutRoutePointsRequest(
                points = listOf(
                    RoutePointInput(latitude = 53.9006, longitude = 27.5590, recordedAt = date.atTimeUtc(7, 0)),
                    RoutePointInput(latitude = 53.9050, longitude = 27.5650, recordedAt = date.atTimeUtc(7, 15)),
                    RoutePointInput(latitude = 53.9100, longitude = 27.5700, recordedAt = date.atTimeUtc(7, 30)),
                ),
            ),
        )
        workoutService.stopWorkout(
            userId,
            session.id,
            StopWorkoutRequest(
                distanceKm = 4.2,
                note = "Утренняя пробежка",
                endedAt = date.atTimeUtc(7, 35),
            ),
        )
    }

    private fun LocalDate.atTimeUtc(hour: Int, minute: Int): OffsetDateTime =
        atTime(hour, minute).atOffset(ZoneOffset.UTC)
}
