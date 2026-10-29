package com.example.application.screens.activity
import com.example.application.dto.*
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test
class BookingAvailabilityTest {
 private val now = LocalDateTime.of(2026,10,28,12,0)
 @Test fun publishedActivitiesForTodayAndFutureDaysWithSeatsCanBeBooked() {
  val dto = ActivityDetailDTO(status = "PUBLISHED", availableSeats = 3, startDate = "2026-10-29T09:00:00")
  assertTrue(ExperienceDetail.Activity(dto).isBookable(now))
  assertFalse(ExperienceDetail.Activity(dto.copy(availableSeats = 0)).isBookable(now))
  assertFalse(ExperienceDetail.Activity(dto.copy(status = "CANCELLED")).isBookable(now))
  assertTrue(ExperienceDetail.Activity(dto.copy(startDate = "2026-10-28T09:00:00")).isBookable(now))
  assertFalse(ExperienceDetail.Activity(dto.copy(startDate = "2026-10-27T09:00:00")).isBookable(now))
 }
 @Test fun tripsFollowBackendInclusiveDateRule() {
  val dto = TripDetailDTO(status = "PUBLISHED", availableSeats = 3, startDate = "2026-10-29")
  assertTrue(ExperienceDetail.Trip(dto).isBookable(now))
  assertTrue(ExperienceDetail.Trip(dto.copy(startDate = "2026-10-28")).isBookable(now))
  assertFalse(ExperienceDetail.Trip(dto.copy(startDate = "2026-10-27")).isBookable(now))
  assertFalse(ExperienceDetail.Trip(dto.copy(status = "DRAFT")).isBookable(now))
 }
 @Test fun bothTypesRemainBookableAt2359ButNotTheFollowingDay() {
  val activity = ExperienceDetail.Activity(ActivityDetailDTO(status = "PUBLISHED", availableSeats = 3, startDate = "2026-10-28T00:00:00"))
  val trip = ExperienceDetail.Trip(TripDetailDTO(status = "PUBLISHED", availableSeats = 3, startDate = "2026-10-28"))
  listOf(activity, trip).forEach {
   assertTrue(it.isBookable(now.toLocalDate().atTime(java.time.LocalTime.MAX)))
   assertFalse(it.isBookable(now.toLocalDate().plusDays(1).atStartOfDay()))
  }
 }
}
