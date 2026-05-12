package com.bsuir.Activity_tracker_server.bodymetrics.exception

import java.util.UUID

class WeightEntryNotFoundException(id: UUID) :
    RuntimeException("Weight entry with id '$id' was not found")
