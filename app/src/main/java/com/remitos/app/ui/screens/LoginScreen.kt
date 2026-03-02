package com.remitos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.R
import com.remitos.app.RemitosApplication
import com.remitos.app.data.DatabaseManager
import com.remitos.app.data.UserInfo
import com.remitos.app.data.db.entity.LocalDeviceEntity
import com.remitos.app.ui.theme.BrandBlue
import com.remitos.app.ui.theme.Blue50
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Login screen for user authentication.
 * Supports email/password login, account switching, and offline mode.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onContinueOffline: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var deviceInfo by remember { mutableStateOf<LocalDeviceEntity?>(null) }
    
    // Load device info from local database
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val db = DatabaseManager.getOfflineDatabase(context)
                deviceInfo = db.localDeviceDao().getDevice()
            } catch (e: Exception) {
                // Device not registered
            }
        }
    }
    
    val viewModel: LoginViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(app.authManager) as T
            }
        }
    )
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    
    // Handle navigation on successful login
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            // Initialize user context before navigating
            app.initializeCurrentUserContext()
            onLoginSuccess()
        }
    }
    
    // Show errors in snackbar
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Error) {
            val message = (uiState as LoginUiState.Error).message
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LoginContent(
            modifier = Modifier.padding(padding),
            deviceInfo = deviceInfo,
            accounts = accounts,
            uiState = uiState,
            onLogin = { companyCode, username, password ->
                scope.launch {
                    viewModel.login(companyCode, username, password)
                }
            },
            onSwitchAccount = { userId ->
                scope.launch {
                    viewModel.switchToAccount(userId)
                    app.initializeCurrentUserContext()
                    onLoginSuccess()
                }
            },
            onContinueOffline = onContinueOffline,
        )
    }
}

@Composable
private fun LoginContent(
    modifier: Modifier = Modifier,
    deviceInfo: LocalDeviceEntity?,
    accounts: List<UserInfo>,
    uiState: LoginUiState,
    onLogin: (String, String, String) -> Unit,
    onSwitchAccount: (String) -> Unit,
    onContinueOffline: () -> Unit,
) {
    var companyCode by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val isLoading = uiState is LoginUiState.Loading
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header with En Punto branding (same as Dashboard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Blue50,
                            Color(0xFFF6F7F9),
                        ),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 48.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_wordmark),
                    contentDescription = "en punto",
                    modifier = Modifier.size(width = 200.dp, height = 52.dp),
                )
                Text(
                    text = "Remitos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandBlue,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        
        // Device info header (if device is registered)
        AnimatedVisibility(
            visible = deviceInfo != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            deviceInfo?.let { device ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandBlue.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Dispositivo registrado",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandBlue,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Empresa: ${device.companyId}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        device.warehouseId?.let { warehouseId ->
                            Text(
                                text = "Warehouse: $warehouseId",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                        Text(
                            text = "ID: ${device.deviceId.take(8)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Login form content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        
        // Account Switcher (if accounts exist)
        AnimatedVisibility(
            visible = accounts.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column {
                Text(
                    text = "Cuentas guardadas",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                
                Box {
                    OutlinedButton(
                        onClick = { showAccountMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cambiar de cuenta")
                    }
                    
                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false },
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(account.email)
                                        account.name?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showAccountMenu = false
                                    onSwitchAccount(account.userId)
                                },
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Company Code field
        OutlinedTextField(
            value = companyCode,
            onValueChange = { companyCode = it.uppercase() },
            label = { Text("Código de empresa") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
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
                    onLogin(companyCode, username, password)
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    enabled = !isLoading,
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                    )
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Login button
        Button(
            onClick = { onLogin(companyCode, username, password) },
            enabled = companyCode.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.height(24.dp),
                )
            } else {
                Text("Iniciar sesión")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Continue offline button
        OutlinedButton(
            onClick = onContinueOffline,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continuar sin conexión")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Version info
        Text(
            text = "v${com.remitos.app.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        }
        
        // Bottom spacer for scroll padding
        Spacer(modifier = Modifier.height(24.dp))
    }
}
