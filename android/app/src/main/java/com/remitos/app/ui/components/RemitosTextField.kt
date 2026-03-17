package com.remitos.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
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
    Branded,    // Blue background, white text
    Reversed,   // White background, blue text/borders/icons
    Surface,    // White background, dark text (default)
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
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    variant: RemitosTextFieldVariant = RemitosTextFieldVariant.Surface,
) {
    val isBranded = variant == RemitosTextFieldVariant.Branded
    val isReversed = variant == RemitosTextFieldVariant.Reversed
    val textColor = when {
        isBranded -> Color.White
        isReversed -> BrandBlue
        else -> MaterialTheme.colorScheme.onSurface
    }
    val labelColor = when {
        isBranded -> Color.White.copy(alpha = 0.8f)
        isReversed -> BrandBlue.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val placeholderColor = when {
        isBranded -> Color.White.copy(alpha = 0.6f)
        isReversed -> BrandBlue.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val borderColor = when {
        isBranded -> Color.White
        isReversed -> BrandBlue
        else -> MaterialTheme.colorScheme.outline
    }
    val iconTint = when {
        isBranded -> Color.White
        isReversed -> BrandBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val cursorColor = when {
        isBranded -> Color.White
        isReversed -> BrandBlue
        else -> MaterialTheme.colorScheme.primary
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder,
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
            supportingText
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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
            focusedPlaceholderColor = placeholderColor,
            unfocusedPlaceholderColor = placeholderColor,
            cursorColor = cursorColor,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
