package com.remitos.app.ui.theme

import androidx.compose.ui.unit.Dp

/* All spacing values are derived from `Tokens.Spacing`, which is generated from
 * design-tokens/tokens.json. Prefer `Tokens.Spacing.*` in new code; the named
 * aliases below exist for compatibility. */
object Spacing {
    val ScreenPadding: Dp = Tokens.Spacing.screen
    val SectionSpacing: Dp = Tokens.Spacing.section
    val ItemSpacing: Dp = Tokens.Spacing.item
    val LargeSpacing: Dp = Tokens.Spacing.large
    val ExtraLargeSpacing: Dp = Tokens.Spacing.xlarge
    val HugeSpacing: Dp = Tokens.Spacing.huge
}
