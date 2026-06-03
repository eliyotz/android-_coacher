package com.coach.screentime.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimeTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    // -------------------------------------------------------------------------
    // localDateString
    // -------------------------------------------------------------------------

    @Test
    fun `localDateString epoch zero is 1970-01-01`() {
        assertEquals("1970-01-01", Time.localDateString(0L, utc))
    }

    @Test
    fun `localDateString known mid-2021 epoch`() {
        // 2021-07-15 00:00:00 UTC = 1626307200000 ms
        assertEquals("2021-07-15", Time.localDateString(1_626_307_200_000L, utc))
    }

    @Test
    fun `localDateString one millisecond before midnight stays on previous day`() {
        // 1970-01-02 00:00:00 UTC = 86400000 ms; one ms before = 86399999
        assertEquals("1970-01-01", Time.localDateString(86_399_999L, utc))
    }

    @Test
    fun `localDateString exactly midnight advances to next day`() {
        // 1970-01-02 00:00:00 UTC
        assertEquals("1970-01-02", Time.localDateString(86_400_000L, utc))
    }

    // -------------------------------------------------------------------------
    // mondayOf
    // -------------------------------------------------------------------------

    @Test
    fun `mondayOf a Monday returns itself`() {
        val monday = LocalDate.of(2024, 1, 1) // 2024-01-01 is a Monday
        assertEquals(monday, Time.mondayOf(monday))
    }

    @Test
    fun `mondayOf a Wednesday returns that week's Monday`() {
        val wednesday = LocalDate.of(2024, 1, 3)   // 2024-01-03 is a Wednesday
        val expectedMonday = LocalDate.of(2024, 1, 1)
        assertEquals(expectedMonday, Time.mondayOf(wednesday))
    }

    @Test
    fun `mondayOf a Sunday returns Monday 6 days earlier`() {
        val sunday = LocalDate.of(2024, 1, 7)   // 2024-01-07 is a Sunday
        val expectedMonday = LocalDate.of(2024, 1, 1)
        assertEquals(expectedMonday, Time.mondayOf(sunday))
    }

    @Test
    fun `mondayOf a Saturday returns Monday 5 days earlier`() {
        val saturday = LocalDate.of(2024, 1, 6)
        val expectedMonday = LocalDate.of(2024, 1, 1)
        assertEquals(expectedMonday, Time.mondayOf(saturday))
    }

    @Test
    fun `mondayOf a Friday returns Monday 4 days earlier`() {
        val friday = LocalDate.of(2024, 1, 5)
        val expectedMonday = LocalDate.of(2024, 1, 1)
        assertEquals(expectedMonday, Time.mondayOf(friday))
    }

    // -------------------------------------------------------------------------
    // daysBetween
    // -------------------------------------------------------------------------

    @Test
    fun `daysBetween same day returns 0`() {
        val date = LocalDate.of(2024, 6, 15)
        assertEquals(0L, Time.daysBetween(date, date))
    }

    @Test
    fun `daysBetween consecutive days returns 1`() {
        val start = LocalDate.of(2024, 6, 15)
        val end   = LocalDate.of(2024, 6, 16)
        assertEquals(1L, Time.daysBetween(start, end))
    }

    @Test
    fun `daysBetween known multi-day span`() {
        val start = LocalDate.of(2024, 1, 1)
        val end   = LocalDate.of(2024, 1, 8)
        assertEquals(7L, Time.daysBetween(start, end))
    }

    @Test
    fun `daysBetween across month boundary`() {
        val start = LocalDate.of(2024, 1, 28)
        val end   = LocalDate.of(2024, 2, 4)
        assertEquals(7L, Time.daysBetween(start, end))
    }

    @Test
    fun `daysBetween reversed order returns negative`() {
        // ChronoUnit.DAYS.between is signed; verifying the sign is correct
        val start = LocalDate.of(2024, 6, 16)
        val end   = LocalDate.of(2024, 6, 15)
        assertEquals(-1L, Time.daysBetween(start, end))
    }

    // -------------------------------------------------------------------------
    // startOfDayMillis / endOfDayMillis
    // -------------------------------------------------------------------------

    @Test
    fun `startOfDayMillis epoch day is 0 for 1970-01-01 in UTC`() {
        val date = LocalDate.of(1970, 1, 1)
        assertEquals(0L, Time.startOfDayMillis(date, utc))
    }

    @Test
    fun `endOfDayMillis equals startOfDay next day minus 1`() {
        val date = LocalDate.of(2024, 6, 15)
        val expected = Time.startOfDayMillis(date.plusDays(1), utc) - 1L
        assertEquals(expected, Time.endOfDayMillis(date, utc))
    }

    @Test
    fun `endOfDayMillis relationship holds for first day of year`() {
        val date = LocalDate.of(2024, 1, 1)
        assertEquals(
            Time.startOfDayMillis(date.plusDays(1), utc) - 1L,
            Time.endOfDayMillis(date, utc)
        )
    }

    @Test
    fun `startOfDayMillis advances exactly 86400000 ms per day in UTC`() {
        val day1 = LocalDate.of(2024, 3, 15)
        val day2 = day1.plusDays(1)
        assertEquals(
            86_400_000L,
            Time.startOfDayMillis(day2, utc) - Time.startOfDayMillis(day1, utc)
        )
    }

    @Test
    fun `startOfDayMillis is divisible by 1000 (whole seconds)`() {
        val date = LocalDate.of(2023, 11, 20)
        assertEquals(0L, Time.startOfDayMillis(date, utc) % 1_000L)
    }

    @Test
    fun `endOfDayMillis modulo 1000 is 999 (one ms before next second)`() {
        val date = LocalDate.of(2023, 11, 20)
        assertEquals(999L, Time.endOfDayMillis(date, utc) % 1_000L)
    }

    // -------------------------------------------------------------------------
    // lastNDaysStrings
    // -------------------------------------------------------------------------

    @Test
    fun `lastNDaysStrings returns list of correct size`() {
        val n = 7
        assertEquals(n, Time.lastNDaysStrings(n, utc).size)
    }

    @Test
    fun `lastNDaysStrings first element equals todayString for same zone`() {
        assertEquals(Time.todayString(utc), Time.lastNDaysStrings(7, utc).first())
    }

    @Test
    fun `lastNDaysStrings list is strictly descending`() {
        val days = Time.lastNDaysStrings(7, utc)
        for (i in 0 until days.size - 1) {
            val current = LocalDate.parse(days[i])
            val next    = LocalDate.parse(days[i + 1])
            assertTrue(
                "Expected ${days[i]} > ${days[i + 1]}",
                current.isAfter(next)
            )
        }
    }

    @Test
    fun `lastNDaysStrings all entries are valid ISO dates`() {
        // LocalDate.parse throws DateTimeParseException if the format is wrong
        val days = Time.lastNDaysStrings(14, utc)
        days.forEach { LocalDate.parse(it) } // must not throw
    }

    @Test
    fun `lastNDaysStrings consecutive entries differ by exactly 1 day`() {
        val days = Time.lastNDaysStrings(5, utc)
        for (i in 0 until days.size - 1) {
            val current = LocalDate.parse(days[i])
            val next    = LocalDate.parse(days[i + 1])
            assertEquals(1L, Time.daysBetween(next, current))
        }
    }

    @Test
    fun `lastNDaysStrings size 1 contains only today`() {
        val result = Time.lastNDaysStrings(1, utc)
        assertEquals(1, result.size)
        assertEquals(Time.todayString(utc), result[0])
    }
}
