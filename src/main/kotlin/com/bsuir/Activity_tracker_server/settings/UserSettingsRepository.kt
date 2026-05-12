package com.bsuir.Activity_tracker_server.settings

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserSettingsRepository : JpaRepository<UserSettings, UUID>
