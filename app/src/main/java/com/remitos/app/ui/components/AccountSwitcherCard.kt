package com.remitos.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remitos.app.data.UserInfo
import kotlinx.coroutines.launch

/**
 * Component showing current user with account switching capability.
 */
@Composable
fun AccountSwitcherCard(
    currentUser: UserInfo?,
    accounts: List<UserInfo>,
    onSwitchAccount: (String) -> Unit,
    onLogout: (Boolean) -> Unit, // Boolean = delete local data
    modifier: Modifier = Modifier,
) {
    var showAccountMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Cuenta actual",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            if (currentUser != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = currentUser.name ?: "Usuario",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Account switcher dropdown
                    if (accounts.size > 1) {
                        Box {
                            IconButton(onClick = { showAccountMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Cambiar cuenta",
                                )
                            }

                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false },
                            ) {
                                accounts
                                    .filter { it.userId != currentUser.userId }
                                    .forEach { account ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(account.name ?: "Usuario")
                                                    Text(
                                                        text = account.email,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                showAccountMenu = false
                                                onSwitchAccount(account.userId)
                                            },
                                        )
                                    }
                            }
                        }
                    }

                    // Logout button
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                // No user logged in
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Modo sin conexión",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = { deleteData ->
                showLogoutDialog = false
                onLogout(deleteData)
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

/**
 * Dialog to confirm logout with option to delete local data.
 */
@Composable
private fun LogoutConfirmDialog(
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var deleteLocalData by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cerrar sesión") },
        text = {
            Column {
                Text("¿Está seguro que desea cerrar sesión?")

                Spacer(modifier = Modifier.padding(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteLocalData = !deleteLocalData },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = deleteLocalData,
                        onCheckedChange = { deleteLocalData = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar datos locales")
                }

                if (deleteLocalData) {
                    Text(
                        text = "⚠️ Esta acción eliminará todos los datos no sincronizados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(deleteLocalData) },
            ) {
                Text(
                    "Cerrar sesión",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
