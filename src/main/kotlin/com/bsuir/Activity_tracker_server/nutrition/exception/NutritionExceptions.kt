package com.bsuir.Activity_tracker_server.nutrition.exception

import java.util.UUID

class MealTypeNotFoundException(mealTypeId: UUID) :
    RuntimeException("Meal type with id '$mealTypeId' was not found")

class FoodItemNotFoundException(foodItemId: UUID) :
    RuntimeException("Food item with id '$foodItemId' was not found")

class NutritionRecordNotFoundException(recordId: UUID) :
    RuntimeException("Nutrition record with id '$recordId' was not found")

class DuplicateMealTypeException(name: String) :
    RuntimeException("Meal type '$name' already exists")

class DuplicateFoodItemException(name: String) :
    RuntimeException("Food item '$name' already exists")

class MealTypeInUseException(mealTypeId: UUID) :
    RuntimeException("Meal type '$mealTypeId' is used by existing records")

class FoodItemInUseException(foodItemId: UUID) :
    RuntimeException("Food item '$foodItemId' is used by existing records")

