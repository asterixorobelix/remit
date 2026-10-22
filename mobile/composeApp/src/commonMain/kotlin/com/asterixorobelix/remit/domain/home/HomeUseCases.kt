package com.asterixorobelix.remit.domain.home

import com.asterixorobelix.remit.util.currentLocalHour

enum class DayPeriod { MORNING, AFTERNOON, EVENING }

/** Greeting period for the Home header. */
class GetDayPeriodUseCase {
    operator fun invoke(hour: Int = currentLocalHour()): DayPeriod = when {
        hour < NOON -> DayPeriod.MORNING
        hour < EVENING_START -> DayPeriod.AFTERNOON
        else -> DayPeriod.EVENING
    }

    private companion object {
        const val NOON = 12
        const val EVENING_START = 17
    }
}
