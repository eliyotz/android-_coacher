package com.coach.screentime.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Time {
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun localDateString(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().format(isoDate)

    fun todayString(zone: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.now(zone).format(isoDate)

    fun lastNDaysStrings(n: Int, zone: ZoneId = ZoneId.systemDefault()): List<String> {
        val today = LocalDate.now(zone)
        return (0 until n).map { today.minusDays(it.toLong()).format(isoDate) }
    }

    fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun mondayOf(date: LocalDate): LocalDate {
        val dow = date.dayOfWeek.value // Mon=1
        return date.minusDays((dow - 1).toLong())
    }

    /** Sundays are when the weekly report runs. Returns Monday of the week being reported on. */
    fun lastCompletedWeekStart(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        mondayOf(LocalDate.now(zone)).minusDays(7)

    fun daysBetween(start: LocalDate, endInclusive: LocalDate): Long =
        ChronoUnit.DAYS.between(start, endInclusive)
}
