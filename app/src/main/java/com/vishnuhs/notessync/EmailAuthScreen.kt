package com.vishnuhs.notessync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailAuthScreen(
    onNavigateBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    authStatus: String
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSignUp) "Create Account" else "Sign In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = if (isSignUp) "Create your account to sync notes across devices" else "Sign in to access your synced notes",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Confirm password field (only for sign up)
            if (isSignUp) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isSignUp && password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword
                )

                if (isSignUp && password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text(
                        text = "Passwords don't match",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auth status
            if (authStatus.isNotEmpty() && authStatus != "Ready") {
                Text(
                    text = authStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (authStatus.contains("failed") || authStatus.contains("error"))
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sign in/up button
            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    val trimmedPassword = password.trim()

                    if (isSignUp) {
                        if (trimmedPassword == confirmPassword.trim() && trimmedEmail.isNotEmpty() && trimmedPassword.length >= 6) {
                            onSignUp(trimmedEmail, trimmedPassword)
                        }
                    } else {
                        if (trimmedEmail.isNotEmpty() && trimmedPassword.isNotEmpty()) {
                            onSignIn(trimmedEmail, trimmedPassword)
                        }
                    }
                },
                enabled = email.isNotEmpty() && password.isNotEmpty() &&
                        (!isSignUp || (password == confirmPassword && password.length >= 6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignUp) "Create Account" else "Sign In")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle sign in/up
            TextButton(
                onClick = {
                    isSignUp = !isSignUp
                    confirmPassword = ""
                }
            ) {
                Text(
                    if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Password requirements (for sign up)
            if (isSignUp) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Password Requirements:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "• At least 6 characters long",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Use a strong, unique password",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}