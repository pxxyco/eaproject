package com.example.application.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.ScreenBackground
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone900

@Composable
fun LoginScreen(onLoginClick: () -> Unit, onSkipClick: () -> Unit)
{
    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground).padding(24.dp),
        contentAlignment = Alignment.Center)
    {
        Column(modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center)
        {
            Text(text = "Benvenuto",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Accedi per continuare o esplora l'app liberamente.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            //Effettua il login
            Button(onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Stone200,
                    contentColor = Stone900))
            {
                Text(text = "Effettua il Login", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            //Annulla / Salta alla schermata principale
            OutlinedButton(onClick = onSkipClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Stone200))
            {
                Text(text = "Salta / Annulla", fontWeight = FontWeight.Bold)
            }
        }
    }
}