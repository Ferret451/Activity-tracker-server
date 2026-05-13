package com.bsuir.Activity_tracker_server.food.exception

class FoodNotFoundException(fdcId: Long) : RuntimeException("Food with id '$fdcId' was not found")

