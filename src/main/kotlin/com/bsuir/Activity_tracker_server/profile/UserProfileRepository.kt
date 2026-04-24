package com.bsuir.Activity_tracker_server.profile

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserProfileRepository : JpaRepository<UserProfile, UUID>

