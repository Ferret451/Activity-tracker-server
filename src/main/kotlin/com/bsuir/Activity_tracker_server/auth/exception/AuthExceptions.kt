package com.bsuir.Activity_tracker_server.auth.exception

class InvalidCredentialsException : RuntimeException("Invalid email or password")

class InvalidRefreshTokenException : RuntimeException("Refresh token is invalid or expired")

class InvalidCurrentPasswordException : RuntimeException("Current password is incorrect")
