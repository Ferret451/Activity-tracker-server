package com.bsuir.Activity_tracker_server.activity

import com.bsuir.Activity_tracker_server.activity.exception.ActivityTypeDoesNotSupportDistanceException

internal fun ActivityType.requireSupportsDistance(distanceKm: Double?) {
    if (!supportsDistance && distanceKm != null) {
        throw ActivityTypeDoesNotSupportDistanceException(name)
    }
}

internal fun ActivityType.requireSupportsDistanceForRoute() {
    if (!supportsDistance) {
        throw ActivityTypeDoesNotSupportDistanceException(name)
    }
}
