package com.remitos.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.remitos.app.R
import com.remitos.app.RemitosApplication
import com.remitos.app.data.AuthManager
import com.remitos.app.data.DatabaseManager
import com.remitos.app.data.TokenData
import com.remitos.app.data.db.entity.LocalDeviceEntity
import com.remitos.app.network.ApiClient
import com.remitos.app.network.LoginRequest
import com.remitos.app.network.RegisterDeviceRequest
import com.remitos.app.network.WarehouseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSetupScreen(
    onDeviceRegistered: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val scope = rememberCoroutineScope()
    
    var currentStep by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var warehouses by remember { mutableStateOf<List<WarehouseDto>>(emptyList()) }
    var authToken by remember { mutableStateOf<String?>(null) }
    
    // Form state
    var companyCode by remember { mutableStateOf("DEMO") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf(android.os.Build.MODEL) }
    var selectedWarehouse by remember { mutableStateOf<WarehouseDto?>(null) }
    var warehouseDropdownExpanded by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    
    when (currentStep) {
        1 -> {
            // Login Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFF6F7F9)),
                            ),
                        )
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_wordmark),
                            contentDescription = "en punto",
                            modifier = Modifier.size(width = 200.dp, height = 52.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Configurar Dispositivo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Inicia sesión como administrador para registrar este dispositivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    errorMessage?.let {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    OutlinedTextField(
                        value = companyCode,
                        onValueChange = { companyCode = it.uppercase() },
                        label = { Text("Código de empresa") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario admin") },
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
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        ApiClient.getUnauthenticatedApiService().login(
                                            LoginRequest(
                                                companyCode = companyCode,
                                                username = username,
                                                password = password
                                            )
                                        )
                                    }
                                    
                                    if (response.isSuccessful && response.body() != null) {
                                        authToken = response.body()!!.token
                                        
                                        // Load warehouses
                                        val warehousesResponse = withContext(Dispatchers.IO) {
                                            ApiClient.getUnauthenticatedApiService().getWarehouses(companyCode)
                                        }
                                        if (warehousesResponse.isSuccessful) {
                                            warehouses = warehousesResponse.body() ?: emptyList()
                                        }
                                        
                                        currentStep = 2
                                    } else {
                                        errorMessage = "Credenciales inválidas"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Error de conexión"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
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
                            Text("Continuar")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = onCancel) {
                        Text("Cancelar")
                    }
                }
            }
        }
        
        2 -> {
            // Assign Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFF6F7F9)),
                            ),
                        )
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_wordmark),
                            contentDescription = "en punto",
                            modifier = Modifier.size(width = 200.dp, height = 52.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Asignar Dispositivo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Selecciona el warehouse para este dispositivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    errorMessage?.let {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Nombre del dispositivo") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = warehouseDropdownExpanded,
                        onExpandedChange = { warehouseDropdownExpanded = !warehouseDropdownExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedWarehouse?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Warehouse") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseDropdownExpanded) },
                            enabled = !isLoading && warehouses.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        
                        ExposedDropdownMenu(
                            expanded = warehouseDropdownExpanded,
                            onDismissRequest = { warehouseDropdownExpanded = false },
                        ) {
                            warehouses.forEach { warehouse ->
                                DropdownMenuItem(
                                    text = { Text(warehouse.name) },
                                    onClick = {
                                        selectedWarehouse = warehouse
                                        warehouseDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    
                    if (warehouses.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cargando warehouses...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            selectedWarehouse?.let { warehouse ->
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val deviceUuid = UUID.randomUUID().toString()
                                        
                                        val response = withContext(Dispatchers.IO) {
                                            ApiClient.getUnauthenticatedApiService().registerDevice(
                                                RegisterDeviceRequest(
                                                    deviceUuid = deviceUuid,
                                                    platform = "android",
                                                    warehouseId = warehouse.id,
                                                    model = deviceName,
                                                    deviceName = deviceName,
                                                    username = username,
                                                    password = password
                                                )
                                            )
                                        }
                                        
                                        if (response.isSuccessful) {
                                            val deviceResponse = response.body()
                                            // Save device to local database
                                            val database = DatabaseManager.getOfflineDatabase(app)
                                            database.localDeviceDao().insert(
                                                LocalDeviceEntity(
                                                    deviceId = deviceUuid,
                                                    companyId = "DEMO",
                                                    warehouseId = warehouse.id,
                                                    registeredAt = System.currentTimeMillis()
                                                )
                                            )
                                            
                                            // If tokens returned, save them for auto-login
                                            deviceResponse?.let { resp ->
                                                if (resp.accessToken != null && resp.refreshToken != null) {
                                                    val expiresAt = System.currentTimeMillis() + (resp.expiresIn ?: 3600) * 1000
                                                    val tokenData = TokenData(
                                                        accessToken = resp.accessToken,
                                                        refreshToken = resp.refreshToken,
                                                        expiresAt = expiresAt,
                                                        userEmail = username,
                                                        userName = username
                                                    )
                                                    app.authManager.saveToken(username.lowercase(), tokenData)
                                                    app.authManager.setCurrentUser(username.lowercase())
                                                }
                                            }
                                            
                                            onDeviceRegistered()
                                        } else {
                                            errorMessage = "Error al registrar dispositivo: ${response.code()}"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Error de conexión"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = selectedWarehouse != null && !isLoading,
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
                            Text("Registrar Dispositivo")
                        }
                    }
                }
            }
        }
    }
}
