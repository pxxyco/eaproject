package com.example.application.screens.account

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import com.example.application.R
import com.example.application.network.SavedCard

@Composable
internal fun CardBrandLogo(brand: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color.White, contentColor = Color(0xFF1A1F71)) {
        Box(Modifier.size(78.dp, 44.dp).padding(6.dp), contentAlignment = Alignment.Center) {
            when (brand) {
                "Mastercard" -> Canvas(Modifier.fillMaxSize()) {
                    val radius = size.height * .45f
                    drawCircle(Color(0xFFEB001B), radius, center = center.copy(x = center.x - radius * .58f))
                    drawCircle(Color(0xFFF79E1B).copy(alpha = .9f), radius, center = center.copy(x = center.x + radius * .58f))
                }
                "Visa" -> Text("VISA", fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 23.sp)
                "Amex" -> Box(Modifier.fillMaxSize().background(Color(0xFF176CA5), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text("AMERICAN\nEXPRESS", color = Color.White, fontSize = 8.sp, lineHeight = 9.sp, fontWeight = FontWeight.Black)
                }
                else -> Text("CARD", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
internal fun WalletCard(card: SavedCard, enabled: Boolean, onRemove: () -> Unit) {
    val colors = when (card.brand) {
        "Visa" -> listOf(Color(0xFF224D50), Color(0xFF152F46))
        "Mastercard" -> listOf(Color(0xFF67463E), Color(0xFF302D42))
        "Amex" -> listOf(Color(0xFF2A6180), Color(0xFF23344F))
        else -> listOf(Color(0xFF42594A), Color(0xFF253B30))
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.Transparent, contentColor = Color.White, shadowElevation = 4.dp) {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(colors))) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(alpha = .06f), size.width * .52f, center.copy(x = size.width, y = 0f))
                    drawCircle(Color.White.copy(alpha = .04f), size.width * .42f, center.copy(x = size.width * .9f, y = size.height))
                }
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        CardBrandLogo(card.brand)
                        Text("EA PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Canvas(Modifier.size(36.dp, 27.dp)) {
                            drawRoundRect(Color(0xFFD6BD83), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
                            drawLine(Color(0xFF796843), center.copy(y = 0f), center.copy(y = size.height), 1.dp.toPx())
                            drawLine(Color(0xFF796843), center.copy(x = 0f), center.copy(x = size.width), 1.dp.toPx())
                        }
                        Text("••••  ••••  ${card.last4}", fontSize = 21.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("INTESTATARIO", fontSize = 9.sp, color = Color.White.copy(alpha = .7f))
                            Text(card.holder, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SCADENZA", fontSize = 9.sp, color = Color.White.copy(alpha = .7f))
                            Text("%02d/%02d".format(card.expiryMonth, card.expiryYear % 100), fontWeight = FontWeight.Bold)
                        }
                    }

                }
            }
        }
        OutlinedButton(onClick = onRemove, enabled = enabled, modifier = Modifier.align(Alignment.End),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = .35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .5f))) {
            Icon(painterResource(R.drawable.ic_trash), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Rimuovi carta")
        }
    }
}
