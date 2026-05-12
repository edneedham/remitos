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
import com.remitos.app.ui.theme.Tokens

enum class RemitosTextFieldVariant {
    /**
     * Brand-blue fill, white text. Reserved for content that sits on top of a
     * white surface and needs to dominate visually (rare; kept for the device
     * setup wizard).
     */
    Branded,

    /**
     * Default neutral field: gray outline, dark text, brand-blue focus ring,
     * brand-red error state. Kept as an alias for backwards compatibility.
     */
    Reversed,

    /**
     * Default neutral field: gray outline, dark text, brand-blue focus ring,
     * brand-red error state. Matches the input visual used on the website.
     */
    Surface,
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

    val textColor = if (isBranded) Color.White else Tokens.Color.surfaceText
    val labelColor = if (isBranded) Color.White.copy(alpha = 0.8f) else Tokens.Color.surfaceTextMuted
    val placeholderColor =
        if (isBranded) Color.White.copy(alpha = 0.6f) else Tokens.Color.surfaceTextMuted.copy(alpha = 0.6f)
    val unfocusedBorderColor = if (isBranded) Color.White else Tokens.Color.surfaceBorder
    val focusedBorderColor = if (isBranded) Color.White else Tokens.Color.brandPrimary
    val focusedLabelColor = if (isBranded) Color.White else Tokens.Color.brandPrimary
    val iconTint = if (isBranded) Color.White else Tokens.Color.brandPrimary
    val cursorColor = if (isBranded) Color.White else Tokens.Color.brandPrimary

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
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = labelColor,
            focusedPlaceholderColor = placeholderColor,
            unfocusedPlaceholderColor = placeholderColor,
            cursorColor = cursorColor,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
