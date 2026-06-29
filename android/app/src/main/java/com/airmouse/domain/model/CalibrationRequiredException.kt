package com.airmouse.domain.model

class CalibrationRequiredException(message: String = "Calibration must be completed before connecting to a server.") : IllegalStateException(message)
