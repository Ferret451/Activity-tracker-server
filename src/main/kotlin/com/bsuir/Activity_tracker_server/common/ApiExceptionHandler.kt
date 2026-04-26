package com.bsuir.Activity_tracker_server.common

import com.bsuir.Activity_tracker_server.activity.exception.ActivityRecordNotFoundException
import com.bsuir.Activity_tracker_server.activity.exception.ActivityTypeInUseException
import com.bsuir.Activity_tracker_server.activity.exception.ActivityTypeNotFoundException
import com.bsuir.Activity_tracker_server.activity.exception.DuplicateActivityTypeException
import com.bsuir.Activity_tracker_server.auth.exception.InvalidCredentialsException
import com.bsuir.Activity_tracker_server.auth.exception.InvalidRefreshTokenException
import com.bsuir.Activity_tracker_server.finance.exception.DuplicateFinanceCategoryException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceCategoryNotFoundException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceRecordNotFoundException
import com.bsuir.Activity_tracker_server.food.exception.ExternalFoodApiException
import com.bsuir.Activity_tracker_server.goal.exception.GoalNotFoundException
import com.bsuir.Activity_tracker_server.nutrition.exception.DuplicateFoodItemException
import com.bsuir.Activity_tracker_server.nutrition.exception.DuplicateMealTypeException
import com.bsuir.Activity_tracker_server.nutrition.exception.FoodItemInUseException
import com.bsuir.Activity_tracker_server.nutrition.exception.FoodItemNotFoundException
import com.bsuir.Activity_tracker_server.nutrition.exception.MealTypeInUseException
import com.bsuir.Activity_tracker_server.nutrition.exception.MealTypeNotFoundException
import com.bsuir.Activity_tracker_server.nutrition.exception.NutritionRecordNotFoundException
import com.bsuir.Activity_tracker_server.profile.exception.ProfileNotFoundException
import com.bsuir.Activity_tracker_server.user.exception.DuplicateUserFieldException
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ActivityTypeNotFoundException::class)
    fun handleActivityTypeNotFound(exception: ActivityTypeNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Activity type was not found")

    @ExceptionHandler(ActivityRecordNotFoundException::class)
    fun handleActivityRecordNotFound(exception: ActivityRecordNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Activity record was not found")

    @ExceptionHandler(DuplicateActivityTypeException::class)
    fun handleDuplicateActivityType(exception: DuplicateActivityTypeException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "Activity type already exists")

    @ExceptionHandler(ActivityTypeInUseException::class)
    fun handleActivityTypeInUse(exception: ActivityTypeInUseException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "Activity type is used by existing records")

    @ExceptionHandler(MealTypeNotFoundException::class)
    fun handleMealTypeNotFound(exception: MealTypeNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Meal type was not found")

    @ExceptionHandler(FoodItemNotFoundException::class)
    fun handleFoodItemNotFound(exception: FoodItemNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Food item was not found")

    @ExceptionHandler(NutritionRecordNotFoundException::class)
    fun handleNutritionRecordNotFound(exception: NutritionRecordNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Nutrition record was not found")

    @ExceptionHandler(DuplicateMealTypeException::class)
    fun handleDuplicateMealType(exception: DuplicateMealTypeException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "Meal type already exists")

    @ExceptionHandler(DuplicateFoodItemException::class)
    fun handleDuplicateFoodItem(exception: DuplicateFoodItemException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "Food item already exists")

    @ExceptionHandler(MealTypeInUseException::class)
    fun handleMealTypeInUse(exception: MealTypeInUseException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "Meal type is used by existing records")

    @ExceptionHandler(FoodItemInUseException::class)
    fun handleFoodItemInUse(exception: FoodItemInUseException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "Food item is used by existing records")

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(exception: UserNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "User was not found")

    @ExceptionHandler(ProfileNotFoundException::class)
    fun handleProfileNotFound(exception: ProfileNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Profile was not found")

    @ExceptionHandler(GoalNotFoundException::class)
    fun handleGoalNotFound(exception: GoalNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Goal was not found")

    @ExceptionHandler(FinanceCategoryNotFoundException::class)
    fun handleFinanceCategoryNotFound(exception: FinanceCategoryNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Finance category was not found")

    @ExceptionHandler(FinanceRecordNotFoundException::class)
    fun handleFinanceRecordNotFound(exception: FinanceRecordNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "Finance record was not found")

    @ExceptionHandler(DuplicateUserFieldException::class)
    fun handleDuplicateUserField(exception: DuplicateUserFieldException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "User already exists")

    @ExceptionHandler(DuplicateFinanceCategoryException::class)
    fun handleDuplicateFinanceCategory(exception: DuplicateFinanceCategoryException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "Finance category already exists")

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(exception: InvalidCredentialsException): ProblemDetail =
        buildProblemDetail(HttpStatus.UNAUTHORIZED, exception.message ?: "Invalid credentials")

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(exception: InvalidRefreshTokenException): ProblemDetail =
        buildProblemDetail(HttpStatus.UNAUTHORIZED, exception.message ?: "Invalid refresh token")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ProblemDetail {
        val errors = exception.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "Invalid value") }

        val detail = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed")
        detail.setProperty("errors", errors)
        return detail
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ProblemDetail =
        buildProblemDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid request")

    @ExceptionHandler(ExternalFoodApiException::class)
    fun handleExternalFoodApi(exception: ExternalFoodApiException): ProblemDetail {
        val status = when (exception.statusCode.value()) {
            400 -> HttpStatus.BAD_REQUEST
            401 -> HttpStatus.UNAUTHORIZED
            404 -> HttpStatus.NOT_FOUND
            429 -> HttpStatus.TOO_MANY_REQUESTS
            503 -> HttpStatus.SERVICE_UNAVAILABLE
            else -> HttpStatus.BAD_GATEWAY
        }

        val detail = buildProblemDetail(
            status,
            "Food provider request failed (status ${exception.statusCode.value()})",
        )

        if (!exception.responseBody.isNullOrBlank()) {
            detail.setProperty("upstreamError", exception.responseBody)
        }

        return detail
    }

    private fun buildProblemDetail(status: HttpStatus, detail: String): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
        problemDetail.type = URI("about:blank")
        problemDetail.title = status.reasonPhrase
        return problemDetail
    }
}
