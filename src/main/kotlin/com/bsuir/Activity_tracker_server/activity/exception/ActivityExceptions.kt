package com.bsuir.Activity_tracker_server.activity.exception

import java.util.UUID

class ActivityTypeNotFoundException(typeId: UUID) :
    RuntimeException("Activity type with id '$typeId' was not found")

class ActivityRecordNotFoundException(recordId: UUID) :
    RuntimeException("Activity record with id '$recordId' was not found")

class DuplicateActivityTypeException(name: String) :
    RuntimeException("Activity type '$name' already exists")

class ActivityTypeInUseException(typeId: UUID) :
    RuntimeException("Activity type '$typeId' is used by existing records")

class WorkoutSessionNotFoundException(sessionId: UUID) :
    RuntimeException("Workout session with id '$sessionId' was not found")

class ActiveWorkoutAlreadyExistsException :
    RuntimeException("User already has an active workout session")

class InvalidWorkoutSessionStateException(message: String) :
    RuntimeException(message)

