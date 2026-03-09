package com.remitos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.remitos.app.R
import com.remitos.app.data.db.entity.LocalDeviceEntity
import com.remitos.app.ui.theme.BrandBlue

@Composable
fun LandscapeLoginLayout(
    companyCode: String,
    username: String,
    password: String,
    passwordVisible: Boolean,
    isOperatorMode: Boolean,
    isLoading: Boolean,
    deviceInfo: LocalDeviceEntity?,
    onCompanyCodeChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibleChange: (Boolean) -> Unit,
    onOperatorModeChange: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onContinueOffline: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // Left side - Header
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFFFFFFF),
                            ),
                        ),
                    )
                    .padding(vertical = 16.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_wordmark),
                        contentDescription = "en punto",
                        modifier = Modifier.size(width = 160.dp, height = 42.dp),
                    )
                    Text(
                        text = "Remitos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = BrandBlue,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            
            // Device info
            deviceInfo?.let { device ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = BrandBlue,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Dispositivo registrado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(
                            text = device.companyId,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                }
            }
        }
        
        // Right side - Form
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Mode switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        onClick = { onOperatorModeChange(false) },
                        shape = MaterialTheme.shapes.small,
                        color = if (!isOperatorMode) BrandBlue else Color.White,
                        border = BorderStroke(1.dp, BrandBlue),
                    ) {
                        Text(
                            text = "Administrador",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (!isOperatorMode) Color.White else BrandBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Surface(
                        onClick = { onOperatorModeChange(true) },
                        shape = MaterialTheme.shapes.small,
                        color = if (isOperatorMode) BrandBlue else Color.White,
                        border = BorderStroke(1.dp, BrandBlue),
                    ) {
                        Text(
                            text = "Operador",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOperatorMode) Color.White else BrandBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Form fields
            OutlinedTextField(
                value = companyCode,
                onValueChange = onCompanyCodeChange,
                label = { Text("Código de empresa") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BrandBlue,
                    focusedLabelColor = BrandBlue,
                    unfocusedLabelColor = BrandBlue,
                ),
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Usuario") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BrandBlue,
                    focusedLabelColor = BrandBlue,
                    unfocusedLabelColor = BrandBlue,
                ),
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                trailingIcon = {
                    IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                            tint = BrandBlue,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BrandBlue,
                    focusedLabelColor = BrandBlue,
                    unfocusedLabelColor = BrandBlue,
                ),
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onLogin,
                enabled = companyCode.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                    )
                } else {
                    Text(if (isOperatorMode) "Iniciar sesión como Operador" else "Iniciar sesión")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onContinueOffline,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continuar sin conexión")
            }
        }
    }
}
