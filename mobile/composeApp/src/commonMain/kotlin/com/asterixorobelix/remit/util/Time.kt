package com.asterixorobelix.remit.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Wall-clock time in epoch milliseconds. */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Hour of day (0-23) in the device time zone. */
@OptIn(ExperimentalTime::class)
fun currentLocalHour(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
