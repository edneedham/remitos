package com.remitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remitos.app.data.db.entity.LocalUserEntity
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showCreateDialog by remember { mutableStateOf(false) }
    var userToResetPassword by remember { mutableStateOf<LocalUserEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers(context)
    }

    Scaffold(
        topBar = {
            RemitosTopBar(
                title = "Gestión de Usuarios",
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Crear Operador")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (uiState.isLoading && uiState.users.isEmpty()) {
                Text("Cargando...")
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.users) { user ->
                    UserCard(
                        user = user,
                        onToggleStatus = { 
                            viewModel.toggleStatus(context, user.id, user.status) 
                        },
                        onResetPassword = {
                            userToResetPassword = user
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOperatorDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { email, password ->
                viewModel.createOperator(context, email, password)
                showCreateDialog = false
            }
        )
    }

    userToResetPassword?.let { user ->
        ResetPasswordDialog(
            user = user,
            onDismiss = { userToResetPassword = null },
            onConfirm = { newPassword ->
                viewModel.resetPassword(context, user.id, newPassword)
                userToResetPassword = null
            }
        )
    }
}

@Composable
fun UserCard(
    user: LocalUserEntity,
    onToggleStatus: () -> Unit,
    onResetPassword: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (user.status == "active") "Activo" else "Suspendido",
                    color = if (user.status == "active") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = "Rol: ${translateRole(user.role)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onResetPassword) {
                    Text("Cambiar Contraseña")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onToggleStatus) {
                    Text(if (user.status == "active") "Suspender" else "Activar")
                }
            }
        }
    }
}

@Composable
fun CreateOperatorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Operador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RemitosTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email / Usuario"
                )
                RemitosTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, password) },
                enabled = email.isNotBlank() && password.length >= 8
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ResetPasswordDialog(
    user: LocalUserEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nueva contraseña para ${user.username}")
                RemitosTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Nueva Contraseña",
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.length >= 8
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun translateRole(role: String): String {
    return when (role) {
        "company_owner" -> "Administrador"
        "warehouse_admin" -> "Jefe de Depósito"
        "operator" -> "Operador"
        "admin" -> "Administrador"
        else -> role.replace("_", " ").capitalize()
    }
}
