package com.bsuir.Activity_tracker_server.goal.exception

import java.util.UUID

class GoalNotFoundException(goalId: UUID) : RuntimeException("Goal with id '$goalId' was not found")

class GoalOwnershipException(goalId: UUID) : RuntimeException("Goal '$goalId' does not belong to current user")

