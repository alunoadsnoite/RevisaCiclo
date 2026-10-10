package br.com.revisaciclo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import br.com.revisaciclo.domain.model.TemaApp

private val EsquemaClaro = lightColorScheme(
    primary = VerdePrimario,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F0B6),
    onPrimaryContainer = VerdePrimarioEscuro,
    secondary = TealSecundario,
    onSecondary = Color.White,
    tertiary = AzulTerciario,
    onTertiary = Color.White,
    background = FundoClaro,
    onBackground = TextoClaro,
    surface = SuperficieClara,
    onSurface = TextoClaro,
    surfaceVariant = SuperficieVarianteClara,
    onSurfaceVariant = Color(0xFF424940),
)

private val EsquemaEscuro = darkColorScheme(
    primary = VerdeClaro,
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB4F0B6),
    secondary = Color(0xFF4DD0C3),
    onSecondary = Color(0xFF003731),
    tertiary = Color(0xFF90CAF9),
    onTertiary = Color(0xFF003258),
    background = FundoEscuro,
    onBackground = TextoEscuro,
    surface = SuperficieEscura,
    onSurface = TextoEscuro,
    surfaceVariant = SuperficieVarianteEscura,
    onSurfaceVariant = Color(0xFFBFC9BD),
)

@Composable
fun RevisaCicloTheme(
    tema: TemaApp = TemaApp.AUTOMATICO,
    content: @Composable () -> Unit,
) {
    val escuro = when (tema) {
        TemaApp.CLARO -> false
        TemaApp.ESCURO -> true
        TemaApp.AUTOMATICO -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (escuro) EsquemaEscuro else EsquemaClaro,
        typography = Tipografia,
        content = content,
    )
}
