package com.remitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundListScreen(onBack: () -> Unit) {
    var driverNombre by remember { mutableStateOf("") }
    var driverApellido by remember { mutableStateOf("") }
    var deliveryNumber by remember { mutableStateOf("") }
    var recipientNombre by remember { mutableStateOf("") }
    var recipientApellido by remember { mutableStateOf("") }
    var recipientDireccion by remember { mutableStateOf("") }
    var recipientTelefono by remember { mutableStateOf("") }
    var cantidadBultos by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva lista de reparto") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Volver") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Datos del chofer")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = driverNombre,
                    onValueChange = { driverNombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = driverApellido,
                    onValueChange = { driverApellido = it },
                    label = { Text("Apellido") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Entrega")
            OutlinedTextField(
                value = deliveryNumber,
                onValueChange = { deliveryNumber = it },
                label = { Text("Nº Entrega") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = recipientNombre,
                    onValueChange = { recipientNombre = it },
                    label = { Text("Nombre destinatario") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = recipientApellido,
                    onValueChange = { recipientApellido = it },
                    label = { Text("Apellido destinatario") },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = recipientDireccion,
                onValueChange = { recipientDireccion = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = recipientTelefono,
                onValueChange = { recipientTelefono = it },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cantidadBultos,
                onValueChange = { cantidadBultos = it },
                label = { Text("Bultos") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { /* TODO: guardar lista y asignaciones */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar lista")
            }
        }
    }
}
