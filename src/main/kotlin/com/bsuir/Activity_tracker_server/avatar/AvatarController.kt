package com.bsuir.Activity_tracker_server.avatar

import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.RequestPart
import java.util.UUID

@RestController
@RequestMapping("/api/me/avatar")
class AvatarController(
    private val userRepository: UserRepository,
    private val avatarLocalStorageService: AvatarLocalStorageService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestPart("file") file: MultipartFile,
    ): AvatarResponse {
        val user = getUser(jwt)

        avatarLocalStorageService.deleteByUrlIfLocal(user.avatarUrl)
        val stored = avatarLocalStorageService.store(user.id, file)

        user.avatarUrl = stored.url
        userRepository.save(user)

        return AvatarResponse(url = stored.url)
    }

    @GetMapping
    fun get(@AuthenticationPrincipal jwt: Jwt): AvatarResponse {
        val user = getUser(jwt)
        val url = user.avatarUrl ?: throw IllegalStateException("Avatar is not set")
        return AvatarResponse(url = url)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAvatar(@AuthenticationPrincipal jwt: Jwt) {
        val user = getUser(jwt)
        avatarLocalStorageService.deleteByUrlIfLocal(user.avatarUrl)
        user.avatarUrl = null
        userRepository.save(user)
    }

    private fun getUser(jwt: Jwt) =
        userRepository.findByIdOrNull(UUID.fromString(jwt.subject))
            ?: throw UserNotFoundException(UUID.fromString(jwt.subject))
}

data class AvatarResponse(
    val url: String,
)

