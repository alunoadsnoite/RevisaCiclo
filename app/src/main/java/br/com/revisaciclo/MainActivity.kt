package br.com.revisaciclo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.navigation.RevisaCicloNavHost
import br.com.revisaciclo.ui.theme.RevisaCicloTheme

class MainActivity : ComponentActivity() {

    private var destinoPendente by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        destinoPendente = intent?.getStringExtra(EXTRA_DESTINO)
        setContent {
            val app = application as RevisaCicloApp
            val preferencias by app.container.preferenciasRepository.preferencias
                .collectAsStateWithLifecycle(initialValue = Preferencias())

            RevisaCicloTheme(tema = preferencias.tema) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RevisaCicloNavHost(
                        destinoInicial = destinoPendente,
                        onDestinoConsumido = { destinoPendente = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destinoPendente = intent.getStringExtra(EXTRA_DESTINO)
    }

    companion object {
        const val EXTRA_DESTINO = "br.com.revisaciclo.extra.DESTINO"
    }
}
