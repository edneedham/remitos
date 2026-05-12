package com.remitos.app.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.remitos.app.ui.theme.Tokens

private const val TERMS_URL = "https://enpunto.com/terminos"
private const val PRIVACY_URL = "https://enpunto.com/privacidad"
private const val CONTACT_URL = "https://enpunto.com/contacto"

/* Footer with the legal links surfaced on the website footer. Used on first-run
 * screens (Login, DeviceSetup) so users can read the terms and privacy policy
 * before they sign in. */
@Composable
fun LegalLinksFooter(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    fun open(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { open(TERMS_URL) }) {
            Text(
                text = "Términos",
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Color.surfaceTextMuted,
            )
        }
        Text(
            text = "·",
            style = MaterialTheme.typography.labelMedium,
            color = Tokens.Color.surfaceTextMuted,
        )
        TextButton(onClick = { open(PRIVACY_URL) }) {
            Text(
                text = "Privacidad",
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Color.surfaceTextMuted,
            )
        }
        Text(
            text = "·",
            style = MaterialTheme.typography.labelMedium,
            color = Tokens.Color.surfaceTextMuted,
        )
        TextButton(onClick = { open(CONTACT_URL) }) {
            Text(
                text = "Contacto",
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Color.surfaceTextMuted,
            )
        }
    }
}
