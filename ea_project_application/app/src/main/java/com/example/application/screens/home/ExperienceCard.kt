package com.example.application.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.*

@Composable
fun ExperienceCard(
    title: String,
    location: String,
    category: String,
    price: String,
    rating: String,
    imageUrl: String,
    dateInfo: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(180.dp)
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PopupSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    )
                    .background(Stone600),
                contentAlignment = Alignment.Center
            ) {
                RemotePhoto(
                    imageUrl = imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = title
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OverlayDark
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 2.dp
                            )
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = Stone400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "⭐ $rating",
                        style = MaterialTheme.typography.bodySmall,
                        color = RatingAmber,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!dateInfo.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Stone300
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "da $price",
                        style = MaterialTheme.typography.titleSmall,
                        color = Stone200,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}