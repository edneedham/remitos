package com.remitos.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.remitos.app.ui.theme.BrandBlue

enum class RemitosTextFieldVariant {
    Branded,  // Blue background, white text
    Surface,   // White background, dark text
}

@Composable
fun RemitosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    variant: RemitosTextFieldVariant = RemitosTextFieldVariant.Surface,
) {
    val isBranded = variant == RemitosTextFieldVariant.Branded
    val textColor = if (isBranded) Color.White else MaterialTheme.colorScheme.onSurface
    val labelColor = if (isBranded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isBranded) Color.White else MaterialTheme.colorScheme.outline
    val iconTint = if (isBranded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else iconTint,
                )
            }
        } else {
            null
        },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        readOnly = readOnly,
        enabled = enabled,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            focusedLabelColor = labelColor,
            unfocusedLabelColor = labelColor,
            cursorColor = if (isBranded) Color.White else MaterialTheme.colorScheme.primary,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
