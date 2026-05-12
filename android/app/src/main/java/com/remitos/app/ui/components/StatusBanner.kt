package com.remitos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.remitos.app.ui.theme.Tokens

enum class StatusBannerVariant { Info, Success, Warning, Error }

private data class BannerColors(
    val background: Color,
    val border: Color,
    val content: Color,
    val icon: Color,
    val iconVector: ImageVector,
)

private fun colorsFor(variant: StatusBannerVariant): BannerColors = when (variant) {
    StatusBannerVariant.Info -> BannerColors(
        background = Tokens.Color.infoSubtle,
        border = Tokens.Color.infoSubtle,
        content = Tokens.Color.infoOn,
        icon = Tokens.Color.info,
        iconVector = Icons.Outlined.Info,
    )
    StatusBannerVariant.Success -> BannerColors(
        background = Tokens.Color.successSubtle,
        border = Tokens.Color.successSubtle,
        content = Tokens.Color.successOn,
        icon = Tokens.Color.success,
        iconVector = Icons.Outlined.CheckCircle,
    )
    StatusBannerVariant.Warning -> BannerColors(
        background = Tokens.Color.warningSubtle,
        border = Tokens.Color.warningSubtle,
        content = Tokens.Color.warningOn,
        icon = Tokens.Color.warning,
        iconVector = Icons.Outlined.WarningAmber,
    )
    StatusBannerVariant.Error -> BannerColors(
        background = Tokens.Color.errorSubtle,
        border = Tokens.Color.errorSubtle,
        content = Tokens.Color.errorOn,
        icon = Tokens.Color.error,
        iconVector = Icons.Outlined.ErrorOutline,
    )
}

@Composable
fun StatusBanner(
    variant: StatusBannerVariant,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val colors = colorsFor(variant)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.sm))
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(Tokens.Radius.sm))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = colors.iconVector,
            contentDescription = null,
            tint = colors.icon,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                if (title != null) {
                    Text(
                        text = title,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = colors.content,
                    )
                }
                Text(
                    text = message,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = colors.content,
                )
            }
        }
    }
}
