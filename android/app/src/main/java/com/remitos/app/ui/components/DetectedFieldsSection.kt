package com.remitos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.remitos.app.ocr.FieldPair
import com.remitos.app.ui.theme.BrandBlue
import com.remitos.app.ui.theme.Spacing

@Composable
fun DetectedFieldsSection(
    fields: List<FieldPair>,
    onFieldChange: (index: Int, label: String, value: String) -> Unit,
    onFieldRemove: (index: Int) -> Unit,
    onAddField: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BrandBlue,
        unfocusedBorderColor = BrandBlue.copy(alpha = 0.5f),
        cursorColor = BrandBlue,
    )

    fields.forEachIndexed { index, pair ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = pair.label,
                onValueChange = { onFieldChange(index, it, pair.value) },
                label = { Text("Campo") },
                modifier = Modifier.weight(0.4f),
                singleLine = true,
                colors = labelColors,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = pair.value,
                onValueChange = { onFieldChange(index, pair.label, it) },
                label = { Text("Valor") },
                modifier = Modifier.weight(0.6f),
                singleLine = true,
                colors = labelColors,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            IconButton(
                onClick = { onFieldRemove(index) },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Eliminar campo",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    OutlinedButton(
        onClick = onAddField,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.size(Spacing.ItemSpacing))
        Text("Agregar campo")
    }
}
