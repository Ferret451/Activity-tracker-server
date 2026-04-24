package com.bsuir.Activity_tracker_server.user.exception

import java.util.UUID

class UserNotFoundException(id: UUID) : RuntimeException("User with id '$id' was not found")

class DuplicateUserFieldException(
    field: String,
    value: String,
) : RuntimeException("User with $field '$value' already exists")
