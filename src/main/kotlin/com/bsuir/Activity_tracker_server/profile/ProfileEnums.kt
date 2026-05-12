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

enum class CalorieGoal(val deltaKcal: Int) {
    WEIGHT_LOSS(-600),
    SLOW_WEIGHT_LOSS(-300),
    MAINTAIN(0),
    WEIGHT_GAIN(300),
    FAST_WEIGHT_GAIN(600),
}

