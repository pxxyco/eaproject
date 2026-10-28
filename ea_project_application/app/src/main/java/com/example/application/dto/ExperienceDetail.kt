package com.example.application.dto

import androidx.compose.runtime.saveable.listSaver

enum class ExperienceType { ACTIVITY, TRIP }

data class ExperienceRef(val type: ExperienceType, val id: Long) {
    companion object {
        val Saver = listSaver<ExperienceRef?, Any>(
            save = { ref -> ref?.let { listOf(it.type.name, it.id)} ?: emptyList() },
            restore = { if(it.isEmpty()) null else
                ExperienceRef(ExperienceType.valueOf(it[0] as String), it[1] as Long)
            }
        )
    }
}

sealed interface ExperienceDetail {
    val ref: ExperienceRef
    val title: String
    val location: String
    val categoryLabel: String
    val averageRating: Double?
    val price: Double?
    val availableSeats: Int?
    val coverUrl: String?
    val description: String?

    data class Activity(val dto: ActivityDetailDTO) : ExperienceDetail {
        override val ref get() = ExperienceRef(ExperienceType.ACTIVITY, dto.activityId ?: 0)
        override val title get() = dto.title ?: "Senza titolo"
        override val location get() = listOfNotNull(dto.placeName, dto.city).filter { it.isNotBlank() }.joinToString(" · ")
        override val categoryLabel get() = dto.category ?: "Attività"
        override val averageRating get() = dto.averageRating
        override val price get() = dto.price
        override val availableSeats get() = dto.availableSeats
        override val coverUrl get() = dto.images.orEmpty().minByOrNull { it.orderIndex ?: Int.MAX_VALUE }?.imageUrl
        override val description get() = dto.description
    }

    data class Trip(val dto: TripDetailDTO) : ExperienceDetail {
        override val ref get() = ExperienceRef(ExperienceType.TRIP, dto.tripId ?: 0)
        override val title get() = dto.title ?: "Senza titolo"
        override val location get() = listOfNotNull(dto.destinationCity, dto.destinationCountry).filter { it.isNotBlank() }.joinToString(", ")
        override val categoryLabel get() = "Viaggio"
        override val averageRating get() = dto.averageRating?.toDouble()
        override val price get() = dto.totalPrice?.toDouble()
        override val availableSeats get() = dto.availableSeats
        override val coverUrl get() = dto.coverPhotoUrl
        override val description get() = dto.description
    }
}
