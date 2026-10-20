package com.example.application.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.application.network.AuthRepository
import com.example.application.network.RegisterRequest
import com.example.application.ui.theme.ScreenBackground
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone900
import kotlinx.coroutines.launch

@Composable
fun AuthWizardScreen(onLoginSuccess: () -> Unit, onBackToHome: () -> Unit) {
    var currentStep by remember { mutableStateOf(AuthStep.LOGIN) }
    var selectedRole by remember { mutableStateOf("TRAVELER") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground).padding(24.dp), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth().wrapContentHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (currentStep != AuthStep.LOGIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        OutlinedButton(
                            onClick = {
                                currentStep = when (currentStep) {
                                    AuthStep.FORGOT_PASSWORD -> AuthStep.LOGIN
                                    AuthStep.ROLE_SELECTION -> AuthStep.LOGIN
                                    AuthStep.REGISTRATION_DETAILS -> AuthStep.ROLE_SELECTION
                                    else -> AuthStep.LOGIN
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Stone200)
                        ) {
                            Text("Torna indietro")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                when (currentStep) {
                    AuthStep.LOGIN -> LoginStepContent(
                        isLoading = isLoading,
                        errorMessage = error,
                        onLoginSubmit = { id, pw ->
                            scope.launch {
                                isLoading = true; error = null
                                error = AuthRepository.login(id, pw)
                                isLoading = false
                                if (error == null) onLoginSuccess()
                            }
                        },
                        onGoToForgotPassword = { currentStep = AuthStep.FORGOT_PASSWORD },
                        onGoToRegister = { currentStep = AuthStep.ROLE_SELECTION }
                    )
                    AuthStep.FORGOT_PASSWORD -> ForgotPasswordStepContent(
                        onSendReset = { currentStep = AuthStep.LOGIN }
                    )
                    AuthStep.ROLE_SELECTION -> RoleSelectionStepContent(
                        onSelectRole = { role ->
                            selectedRole = role
                            currentStep = AuthStep.REGISTRATION_DETAILS
                        }
                    )
                    AuthStep.REGISTRATION_DETAILS -> RegistrationStepContent(
                        role = selectedRole,
                        isLoading = isLoading,
                        serverError = error,
                        onRegisterSubmit = { first, last, username, email, password, _ ->
                            scope.launch {
                                isLoading = true; error = null
                                error = AuthRepository.register(
                                    RegisterRequest(username.trim(), email.trim(), "$first $last".trim(), password, selectedRole)
                                )
                                isLoading = false
                                if (error == null) onLoginSuccess()
                            }
                        }
                    )
                }
            }
        }
    }
}

//STEP LOGIN
@Composable
fun LoginStepContent(
    isLoading: Boolean,
    errorMessage: String?,
    onLoginSubmit: (String, String) -> Unit,
    onGoToForgotPassword: () -> Unit,
    onGoToRegister: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Login", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = identifier, onValueChange = { identifier = it }, label = { Text("Email o Username") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onGoToForgotPassword,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Stone200)
        ) {
            Text("Password dimenticata?")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onLoginSubmit(identifier, password) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Stone200, contentColor = Stone900)
        ) { Text("Log In", fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoToRegister,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Stone200)
        ) {
            Text("Non hai un Account? Registrati")
        }
    }
}

//STEP PASSWORD DIMENTICATA
@Composable
fun ForgotPasswordStepContent(onSendReset: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Recupera Password", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Inserisci la tua email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSendReset(email) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Stone200, contentColor = Stone900)
        ) { Text("Invia Richiesta", fontWeight = FontWeight.Bold) }
    }
}

//STEP SELEZIONE RUOLO
@Composable
fun RoleSelectionStepContent(onSelectRole: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Seleziona Account",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSelectRole("TRAVELER") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Stone200, contentColor = Stone900)
        ) {
            Text("Account Personal (Traveler)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSelectRole("ORGANIZER") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Stone200, contentColor = Stone900)
        ) {
            Text("Account Business (Organizer)", fontWeight = FontWeight.Bold)
        }
    }
}



//STEP DETTAGLI REGISTRAZIONE
@Composable
fun RegistrationStepContent(
    isLoading: Boolean,
    serverError: String?,
    role: String,
    onRegisterSubmit: (String, String, String, String, String, String) -> Unit,

) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (role == "ORGANIZER") "Profilo Business" else "Profilo Personal",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Nome") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Cognome") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Conferma Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (password != confirmPassword) {
                    errorMessage = "Le password non coincidono."
                } else {
                    errorMessage = null
                    onRegisterSubmit(firstName, lastName, username, email, password, confirmPassword)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Stone200, contentColor = Stone900)
        ) { Text("Conferma e Registrati", fontWeight = FontWeight.Bold) }
    }
}