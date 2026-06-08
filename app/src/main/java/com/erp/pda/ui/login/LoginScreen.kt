package com.erp.pda.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erp.pda.data.api.SessionManager
import com.erp.pda.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val alreadyLoggedIn = remember { SessionManager.isLoggedIn() }
    LaunchedEffect(Unit) {
        if (alreadyLoggedIn) onLoginSuccess()
    }

    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosGray6),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // ── Logo ──
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = IosBlue.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Store,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ERP 系統",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = IosLabel
        )
        Text(
            text = "進銷存管理平台",
            style = MaterialTheme.typography.bodyMedium,
            color = IosSecondaryLabel
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Login Form ──
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = MaterialTheme.shapes.large,
            color = IosWhite
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Email
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("電郵") },
                    placeholder = { Text("your@email.com", color = IosTertiaryLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = iosTextFieldColors(),
                    shape = MaterialTheme.shapes.small
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("密碼") },
                    placeholder = { Text("輸入密碼", color = IosTertiaryLabel) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = IosGray2
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = iosTextFieldColors(),
                    shape = MaterialTheme.shapes.small
                )

                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = IosRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button — iOS style rounded
                Button(
                    onClick = viewModel::login,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IosBlue,
                        disabledContainerColor = IosBlue.copy(alpha = 0.4f)
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = IosWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("登入", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.7f))
    }
}

@Composable
fun iosTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IosBlue,
    unfocusedBorderColor = IosGray4,
    focusedLabelColor = IosBlue,
    unfocusedLabelColor = IosGray2,
    cursorColor = IosBlue,
    focusedContainerColor = IosWhite,
    unfocusedContainerColor = IosWhite
)
