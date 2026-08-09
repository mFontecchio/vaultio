package com.mrhayami.vaultio.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class VaultioPreviews

@Composable
fun VaultioPreview(content: @Composable () -> Unit) {
    VaultioTheme(dynamicColor = false) {
        Surface {
            content()
        }
    }
}
