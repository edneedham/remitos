package com.remitos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remitos.app.ui.components.LoadingButton
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTextFieldVariant
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.StatusBanner
import com.remitos.app.ui.components.StatusBannerVariant
import com.remitos.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
) {
    val viewModel: ForgotPasswordViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var companyCode by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is ForgotPasswordUiState.Success) {
            // Stay on screen; message shown below
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            RemitosTopBar(
                title = "¿Olvidaste tu contraseña?",
                onBack = onBack,
                showLogo = false,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.SectionSpacing),
        ) {
            Text(
                text = "Ingresá el código de empresa y el mismo usuario o correo que usás para iniciar sesión. Si hay una cuenta con correo registrado, te enviamos un enlace.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState is ForgotPasswordUiState.Success) {
                StatusBanner(
                    variant = StatusBannerVariant.Success,
                    message = "Si los datos coinciden con una cuenta, revisá tu correo (y spam) para el enlace de restablecimiento.",
                )
            }

            if (uiState is ForgotPasswordUiState.Error) {
                StatusBanner(
                    variant = StatusBannerVariant.Error,
                    message = (uiState as ForgotPasswordUiState.Error).message,
                )
            }

            RemitosTextField(
                value = companyCode,
                onValueChange = { companyCode = it.uppercase() },
                label = "Código de empresa",
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                enabled = uiState !is ForgotPasswordUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
                variant = RemitosTextFieldVariant.Reversed,
            )

            RemitosTextField(
                value = username,
                onValueChange = { username = it },
                label = "Correo o usuario",
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.submit(companyCode, username)
                    },
                ),
                enabled = uiState !is ForgotPasswordUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
                variant = RemitosTextFieldVariant.Reversed,
            )

            LoadingButton(
                text = "Enviar instrucciones",
                onClick = { viewModel.submit(companyCode, username) },
                enabled = companyCode.isNotBlank() && username.isNotBlank(),
                isLoading = uiState is ForgotPasswordUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "El enlace del correo abre el sitio web para elegir una contraseña nueva.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al inicio de sesión")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
