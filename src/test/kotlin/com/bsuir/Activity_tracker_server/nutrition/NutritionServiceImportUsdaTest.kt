package com.bsuir.Activity_tracker_server.nutrition

import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.bsuir.Activity_tracker_server.food.client.UsdaNutrientDescriptor
import com.bsuir.Activity_tracker_server.profile.ProfileService
import com.bsuir.Activity_tracker_server.user.User
import com.bsuir.Activity_tracker_server.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class NutritionServiceImportUsdaTest {

    @Test
    fun `importFoodItemFromUsda creates new food item with mapped nutrients`() {
        val userRepository = mock(UserRepository::class.java)
        val mealTypeRepository = mock(MealTypeRepository::class.java)
        val foodItemRepository = mock(FoodItemRepository::class.java)
        val foodItemNutrientRepository = mock(FoodItemNutrientRepository::class.java)
        val nutritionRecordRepository = mock(NutritionRecordRepository::class.java)
        val nutritionRecordNutrientRepository = mock(NutritionRecordNutrientRepository::class.java)
        val foodDataClient = mock(FoodDataClient::class.java)
        val profileService = mock(ProfileService::class.java)

        val userId = UUID.randomUUID()
        val user = testUser(userId)
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(foodItemRepository.findByUser_IdAndFdcId(userId, 12345L)).thenReturn(null)
        `when`(foodItemRepository.existsByUser_IdAndNameIgnoreCase(userId, "Apple")).thenReturn(false)
        `when`(foodDataClient.getFoodById(12345L)).thenReturn(
            UsdaFoodItemResponse(
                fdcId = 12345L,
                description = "Apple",
                foodNutrients = listOf(
                    UsdaFoodNutrient(number = "1008", amount = 52.12),
                    UsdaFoodNutrient(number = "1003", amount = 0.26),
                    UsdaFoodNutrient(number = "1004", amount = 0.17),
                    UsdaFoodNutrient(number = "1005", amount = 13.81),
                    UsdaFoodNutrient(
                        nutrient = UsdaNutrientDescriptor(number = "1079", name = "Fiber, total dietary", unitName = "g"),
                        amount = 2.4,
                    ),
                ),
            ),
        )
        `when`(foodItemRepository.save(any(FoodItem::class.java))).thenAnswer { invocation ->
            val foodItem = invocation.arguments[0] as FoodItem
            foodItem.createdAt = now
            foodItem.updatedAt = now
            foodItem
        }

        val service = NutritionService(
            userRepository = userRepository,
            mealTypeRepository = mealTypeRepository,
            foodItemRepository = foodItemRepository,
            foodItemNutrientRepository = foodItemNutrientRepository,
            nutritionRecordRepository = nutritionRecordRepository,
            nutritionRecordNutrientRepository = nutritionRecordNutrientRepository,
            foodDataClient = foodDataClient,
            profileService = profileService,
        )

        val result = service.importFoodItemFromUsda(userId, 12345L)

        assertEquals("Apple", result.name)
        assertEquals("USDA", result.source)
        assertEquals(12345L, result.fdcId)
        assertEquals(52.12, result.caloriesPer100g)
        assertEquals(0.26, result.proteinPer100g)
        assertEquals(0.17, result.fatPer100g)
        assertEquals(13.81, result.carbsPer100g)
        assertEquals("Imported from USDA", result.note)
        assertEquals(1, result.additionalNutrientsPer100g.size)
        assertEquals("1079", result.additionalNutrientsPer100g.single().number)
        assertEquals(2.4, result.additionalNutrientsPer100g.single().amountPer100g)
    }

    @Test
    fun `importFoodItemFromUsda updates existing item by fdcId`() {
        val userRepository = mock(UserRepository::class.java)
        val mealTypeRepository = mock(MealTypeRepository::class.java)
        val foodItemRepository = mock(FoodItemRepository::class.java)
        val foodItemNutrientRepository = mock(FoodItemNutrientRepository::class.java)
        val nutritionRecordRepository = mock(NutritionRecordRepository::class.java)
        val nutritionRecordNutrientRepository = mock(NutritionRecordNutrientRepository::class.java)
        val foodDataClient = mock(FoodDataClient::class.java)
        val profileService = mock(ProfileService::class.java)

        val userId = UUID.randomUUID()
        val user = testUser(userId)
        val existingItemId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val existingItem = FoodItem(
            id = existingItemId,
            user = user,
            name = "Old name",
            source = "manual",
            fdcId = 999L,
            caloriesPer100g = bd("10.00"),
            proteinPer100g = bd("1.00"),
            fatPer100g = bd("2.00"),
            carbsPer100g = bd("3.00"),
            note = null,
            createdAt = now,
            updatedAt = now,
        )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(foodItemRepository.findByUser_IdAndFdcId(userId, 54321L)).thenReturn(existingItem)
        `when`(foodItemRepository.findByIdAndUser_Id(existingItemId, userId)).thenReturn(existingItem)
        `when`(foodDataClient.getFoodById(54321L)).thenReturn(
            UsdaFoodItemResponse(
                fdcId = 54321L,
                description = "Banana",
                foodNutrients = listOf(
                    UsdaFoodNutrient(number = "1008", amount = 88.0),
                ),
            ),
        )
        `when`(foodItemRepository.save(eq(existingItem))).thenReturn(existingItem)

        val service = NutritionService(
            userRepository = userRepository,
            mealTypeRepository = mealTypeRepository,
            foodItemRepository = foodItemRepository,
            foodItemNutrientRepository = foodItemNutrientRepository,
            nutritionRecordRepository = nutritionRecordRepository,
            nutritionRecordNutrientRepository = nutritionRecordNutrientRepository,
            foodDataClient = foodDataClient,
            profileService = profileService,
        )

        val result = service.importFoodItemFromUsda(userId, 54321L)

        assertEquals("Banana", result.name)
        assertEquals("USDA", result.source)
        assertEquals(999L, result.fdcId)
        assertEquals(88.0, result.caloriesPer100g)
        assertEquals(0.0, result.proteinPer100g)
        assertEquals(0.0, result.fatPer100g)
        assertEquals(0.0, result.carbsPer100g)
        assertEquals("Imported from USDA", result.note)

        verify(foodItemRepository).save(existingItem)
    }

    @Test
    fun `importFoodItemFromUsda refresh does not treat own FDC suffixed name as duplicate`() {
        val userRepository = mock(UserRepository::class.java)
        val mealTypeRepository = mock(MealTypeRepository::class.java)
        val foodItemRepository = mock(FoodItemRepository::class.java)
        val foodItemNutrientRepository = mock(FoodItemNutrientRepository::class.java)
        val nutritionRecordRepository = mock(NutritionRecordRepository::class.java)
        val nutritionRecordNutrientRepository = mock(NutritionRecordNutrientRepository::class.java)
        val foodDataClient = mock(FoodDataClient::class.java)
        val profileService = mock(ProfileService::class.java)

        val userId = UUID.randomUUID()
        val user = testUser(userId)
        val existingItemId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val existingItem = FoodItem(
            id = existingItemId,
            user = user,
            name = "Apple (FDC 12345)",
            source = "USDA",
            fdcId = 12345L,
            caloriesPer100g = bd("52.00"),
            proteinPer100g = bd("0.26"),
            fatPer100g = bd("0.17"),
            carbsPer100g = bd("13.81"),
            note = "Imported from USDA",
            createdAt = now,
            updatedAt = now,
        )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(foodItemRepository.findByUser_IdAndFdcId(userId, 12345L)).thenReturn(existingItem)
        `when`(foodItemRepository.findByIdAndUser_Id(existingItemId, userId)).thenReturn(existingItem)
        `when`(foodDataClient.getFoodById(12345L)).thenReturn(
            UsdaFoodItemResponse(
                fdcId = 12345L,
                description = "Apple",
                foodNutrients = listOf(
                    UsdaFoodNutrient(number = "1008", amount = 52.12),
                    UsdaFoodNutrient(number = "1003", amount = 0.26),
                    UsdaFoodNutrient(number = "1004", amount = 0.17),
                    UsdaFoodNutrient(number = "1005", amount = 13.81),
                ),
            ),
        )
        `when`(foodItemRepository.existsByUser_IdAndNameIgnoreCaseAndIdNot(userId, "Apple", existingItemId)).thenReturn(true)
        `when`(foodItemRepository.existsByUser_IdAndNameIgnoreCaseAndIdNot(userId, "Apple (FDC 12345)", existingItemId)).thenReturn(false)
        `when`(foodItemRepository.save(eq(existingItem))).thenReturn(existingItem)

        val service = NutritionService(
            userRepository = userRepository,
            mealTypeRepository = mealTypeRepository,
            foodItemRepository = foodItemRepository,
            foodItemNutrientRepository = foodItemNutrientRepository,
            nutritionRecordRepository = nutritionRecordRepository,
            nutritionRecordNutrientRepository = nutritionRecordNutrientRepository,
            foodDataClient = foodDataClient,
            profileService = profileService,
        )

        val result = service.importFoodItemFromUsda(userId, 12345L)

        assertEquals("Apple (FDC 12345)", result.name)
        assertEquals(12345L, result.fdcId)
        verify(foodItemRepository).save(existingItem)
    }

    private fun testUser(userId: UUID) = User(
        id = userId,
        email = "test@example.com",
        username = "test_user",
        firstName = "Test",
        lastName = "User",
        passwordHash = "hash",
    )

    private fun bd(value: String): BigDecimal = BigDecimal(value)
}

