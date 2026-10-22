package br.com.revisaciclo.ui.common

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Read configuration so language changes also invalidate labels captured by callbacks. */
@Composable
@ReadOnlyComposable
fun recursosLocalizados(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}
