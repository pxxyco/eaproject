package com.example.application.screens.home

internal data class ActivityCategory(
    val title: String,
    val backendCategory: String
)

internal val activityCategories = listOf(
    ActivityCategory("Escursioni e Trekking", "EXCURSION"),
    ActivityCategory("Visite Culturali e Musei", "VISIT"),
    ActivityCategory("Hotel e Soggiorni", "HOTEL"),
    ActivityCategory("Ristorazione e Degustazioni", "MEAL"),
    ActivityCategory("Trasporti e Mobilità", "TRANSPORT")
)
