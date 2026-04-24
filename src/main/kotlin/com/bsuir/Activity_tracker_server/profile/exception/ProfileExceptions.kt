package com.bsuir.Activity_tracker_server.profile.exception

import java.util.UUID

class ProfileNotFoundException(userId: UUID) : RuntimeException("Profile for user '$userId' was not found")

