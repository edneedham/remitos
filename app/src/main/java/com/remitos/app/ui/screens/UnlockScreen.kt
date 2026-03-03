package com.remitos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.remitos.app.ui.theme.BrandBlue
import com.remitos.app.ui.theme.Blue50

@Composable
fun UnlockScreen(
    userName: String,
    onUnlock: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Blue50, Color(0xFFF6F7F9)),
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Sesión bloqueada",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = BrandBlue,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = userName,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray,
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN/Password field
        OutlinedTextField(
            value = pin,
            onValueChange = { 
                pin = it
                errorMessage = null
            },
            label = { Text("PIN") },
            placeholder = { Text("Ingrese su PIN") },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
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
                    if (pin.isNotEmpty()) {
                        onUnlock(pin)
                    }
                }
            ),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Unlock button
        Button(
            onClick = {
                if (pin.isNotEmpty()) {
                    onUnlock(pin)
                } else {
                    errorMessage = "Ingrese su PIN"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .height(48.dp),
        ) {
            Text("Desbloquear")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logout button
        TextButton(onClick = onLogout) {
            Text("Cerrar sesión")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Inactividad detectada",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp),
        )
    }
}
