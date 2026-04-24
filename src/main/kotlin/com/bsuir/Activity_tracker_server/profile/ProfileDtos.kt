package com.bsuir.Activity_tracker_server.profile

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

data class UpsertProfileRequest(
    @field:Positive
    val weightKg: Double,

    @field:Min(50)
    @field:Max(300)
    val heightCm: Int,

    @field:Min(10)
    @field:Max(120)
    val age: Int,

    val sex: Sex,

    val activityLevel: ActivityLevel,
)

data class UserProfileResponse(
    val weightKg: Double,
    val heightCm: Int,
    val age: Int,
    val sex: Sex,
    val activityLevel: ActivityLevel,
)

data class BmrResponse(
    val bmr: Double,
    val dailyCalories: Double,
)

fun UserProfile.toResponse(): UserProfileResponse = UserProfileResponse(
    weightKg = weightKg,
    heightCm = heightCm,
    age = age,
    sex = sex,
    activityLevel = activityLevel,
)


