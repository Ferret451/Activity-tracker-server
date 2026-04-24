package com.bsuir.Activity_tracker_server.profile

import com.bsuir.Activity_tracker_server.profile.exception.ProfileNotFoundException
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProfileService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): UserProfileResponse {
        val profile = userProfileRepository.findByIdOrNull(userId)
            ?: throw ProfileNotFoundException(userId)

        return profile.toResponse()
    }

    @Transactional
    fun upsertProfile(userId: UUID, request: UpsertProfileRequest): UserProfileResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val profile = userProfileRepository.findByIdOrNull(userId)
            ?.apply {
                weightKg = request.weightKg
                heightCm = request.heightCm
                age = request.age
                sex = request.sex
                activityLevel = request.activityLevel
            }
            ?: UserProfile(
                userId = userId,
                user = user,
                weightKg = request.weightKg,
                heightCm = request.heightCm,
                age = request.age,
                sex = request.sex,
                activityLevel = request.activityLevel,
            )

        return userProfileRepository.save(profile).toResponse()
    }

    @Transactional(readOnly = true)
    fun calculateBmr(userId: UUID): BmrResponse {
        val profile = userProfileRepository.findByIdOrNull(userId)
            ?: throw ProfileNotFoundException(userId)

        val bmr = BmrCalculator.calculateBmr(
            sex = profile.sex,
            weightKg = profile.weightKg,
            heightCm = profile.heightCm,
            age = profile.age,
        )
        val dailyCalories = BmrCalculator.calculateDailyCalories(bmr, profile.activityLevel)

        return BmrResponse(
            bmr = bmr,
            dailyCalories = dailyCalories,
        )
    }
}


