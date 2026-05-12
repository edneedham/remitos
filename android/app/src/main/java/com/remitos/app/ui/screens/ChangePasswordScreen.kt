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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remitos.app.RemitosApplication
import com.remitos.app.ui.components.LoadingButton
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTextFieldVariant
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.StatusBanner
import com.remitos.app.ui.components.StatusBannerVariant
import com.remitos.app.ui.theme.Spacing
import com.remitos.app.ui.theme.Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onPasswordChanged: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: ChangePasswordViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var didFinishLogout by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState !is ChangePasswordUiState.Success || didFinishLogout) {
            return@LaunchedEffect
        }
        didFinishLogout = true
        app.logoutCurrentUser(deleteLocalData = false)
        onPasswordChanged()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            RemitosTopBar(
                title = "Cambiar contraseña",
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
                text = "Al guardar, cerramos la sesión en todos los dispositivos. Volvé a iniciar sesión con la nueva clave.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState is ChangePasswordUiState.Error) {
                StatusBanner(
                    variant = StatusBannerVariant.Error,
                    message = (uiState as ChangePasswordUiState.Error).message,
                )
            }

            RemitosTextField(
                value = current,
                onValueChange = { current = it },
                label = "Contraseña actual",
                singleLine = true,
                visualTransformation = if (showCurrent) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                trailingIcon = {
                    IconButton(onClick = { showCurrent = !showCurrent }) {
                        Icon(
                            imageVector = if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Tokens.Color.brandPrimary,
                        )
                    }
                },
                enabled = uiState !is ChangePasswordUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
                variant = RemitosTextFieldVariant.Reversed,
            )

            RemitosTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = "Nueva contraseña",
                singleLine = true,
                visualTransformation = if (showNew) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                trailingIcon = {
                    IconButton(onClick = { showNew = !showNew }) {
                        Icon(
                            imageVector = if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Tokens.Color.brandPrimary,
                        )
                    }
                },
                enabled = uiState !is ChangePasswordUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
                variant = RemitosTextFieldVariant.Reversed,
            )

            RemitosTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "Confirmar nueva contraseña",
                singleLine = true,
                visualTransformation = if (showConfirm) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (newPass == confirm && newPass.length in 8..72) {
                            viewModel.submit(current, newPass)
                        }
                    },
                ),
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(
                            imageVector = if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Tokens.Color.brandPrimary,
                        )
                    }
                },
                enabled = uiState !is ChangePasswordUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
                variant = RemitosTextFieldVariant.Reversed,
            )

            LoadingButton(
                text = "Actualizar contraseña",
                onClick = {
                    when {
                        newPass.length !in 8..72 -> { /* snackbar optional */ }
                        newPass != confirm -> { }
                        else -> viewModel.submit(current, newPass)
                    }
                },
                enabled = current.isNotBlank() &&
                    newPass.length in 8..72 &&
                    newPass == confirm &&
                    uiState !is ChangePasswordUiState.Loading,
                isLoading = uiState is ChangePasswordUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
