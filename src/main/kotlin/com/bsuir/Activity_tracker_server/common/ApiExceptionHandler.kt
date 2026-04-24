package com.bsuir.Activity_tracker_server.common

import com.bsuir.Activity_tracker_server.auth.exception.InvalidCredentialsException
import com.bsuir.Activity_tracker_server.auth.exception.InvalidRefreshTokenException
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

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(exception: UserNotFoundException): ProblemDetail =
        buildProblemDetail(HttpStatus.NOT_FOUND, exception.message ?: "User was not found")

    @ExceptionHandler(DuplicateUserFieldException::class)
    fun handleDuplicateUserField(exception: DuplicateUserFieldException): ProblemDetail =
        buildProblemDetail(HttpStatus.CONFLICT, exception.message ?: "User already exists")

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

    private fun buildProblemDetail(status: HttpStatus, detail: String): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
        problemDetail.type = URI("about:blank")
        problemDetail.title = status.reasonPhrase
        return problemDetail
    }
}
