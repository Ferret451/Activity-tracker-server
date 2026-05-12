package com.bsuir.Activity_tracker_server.settings

import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class UserSettingsResponse(
    val preferences: Map<String, Any?>,
)

data class UserSettingsUpdateRequest(
    val preferences: Map<String, Any?>,
)

@Service
class UserSettingsService(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
) {

    @Transactional(readOnly = true)
    fun getSettings(userId: UUID): UserSettingsResponse {
        userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val row = userSettingsRepository.findByIdOrNull(userId)
        return UserSettingsResponse(preferences = row?.preferencesJson?.toMap() ?: emptyMap())
    }

    @Transactional
    fun putSettings(userId: UUID, request: UserSettingsUpdateRequest): UserSettingsResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val existing = userSettingsRepository.findByIdOrNull(userId)
        if (existing == null) {
            val created =
                UserSettings(
                    userId = user.id,
                    user = user,
                    preferencesJson = request.preferences.toMutableMap(),
                )
            userSettingsRepository.save(created)
        } else {
            existing.preferencesJson.clear()
            existing.preferencesJson.putAll(request.preferences)
            userSettingsRepository.save(existing)
        }
        return UserSettingsResponse(preferences = request.preferences)
    }
}
