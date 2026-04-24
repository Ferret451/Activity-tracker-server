package com.bsuir.Activity_tracker_server.profile

enum class Sex {
    MALE,
    FEMALE,
}

enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    HIGH(1.725),
    VERY_HIGH(1.9),
}

