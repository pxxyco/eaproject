package com.example.application.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.screens.home.ExperienceCard
import com.example.application.screens.home.SearchTopBar
import com.example.application.screens.home.TripCard
import com.example.application.ui.theme.ScreenBackground

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    val verticalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(verticalScrollState)
            .padding(16.dp)
    ) {
        SearchTopBar(
            searchQuery = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearchSubmit = { /* Azione ricerca */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle(title = "Le Migliori Attività")
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(6) { index ->
                ExperienceCard(
                    title = "Attività Top del Mese #$index",
                    location = "Cosenza / Roma",
                    category = "Attività",
                    price = "45€",
                    rating = "4.9",
                    imageUrl = "",
                    dateInfo = "Disponibile",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionTitle(title = "I Migliori Viaggi in Primo Piano")
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(4) { index ->
                TripCard(
                    title = "Pacchetto Viaggio Esclusivo $index",
                    location = "Tour della Calabria & Sicilia",
                    price = "320€",
                    rating = "4.9",
                    dateRange = "12 Giu - 19 Giu",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        //LISTE PER CIASCUNA CATEGORIA DI ATTIVITÀ

        SectionTitle(title = "Escursioni e Trekking")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(5) { index ->
                ExperienceCard(
                    title = "Sentiero della Sila #$index",
                    location = "Parco Nazionale",
                    category = "Escursione",
                    price = "25€",
                    rating = "4.8",
                    imageUrl = "",
                    dateInfo = "Weekend",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionTitle(title = "Visite Culturali e Musei")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(5) { index ->
                ExperienceCard(
                    title = "Visita Guidata Storica #$index",
                    location = "Centro Storico",
                    category = "Visita",
                    price = "20€",
                    rating = "4.7",
                    imageUrl = "",
                    dateInfo = "Domani",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionTitle(title = "Hotel e Soggiorni")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(5) { index ->
                ExperienceCard(
                    title = "Resort & Spa Partner #$index",
                    location = "Tropea",
                    category = "Hotel",
                    price = "90€",
                    rating = "4.9",
                    imageUrl = "",
                    dateInfo = null,
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionTitle(title = "Ristorazione e Degustazioni")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(5) { index ->
                ExperienceCard(
                    title = "Cena Tipica Tradizionale #$index",
                    location = "Ristorante Locale",
                    category = "Ristorazione",
                    price = "35€",
                    rating = "4.9",
                    imageUrl = "",
                    dateInfo = "Stasera",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionTitle(title = "Trasporti e Mobilità")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(4) { index ->
                ExperienceCard(
                    title = "Transfer Privato #$index",
                    location = "Aeroporto - Hotel",
                    category = "Trasporto",
                    price = "45€",
                    rating = "4.6",
                    imageUrl = "",
                    dateInfo = "Su misura",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontSize = 21.sp,
        fontWeight = FontWeight.ExtraBold
    )
}