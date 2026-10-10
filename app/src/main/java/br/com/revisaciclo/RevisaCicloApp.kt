package br.com.revisaciclo

import android.app.Application
import br.com.revisaciclo.notificacao.CanalNotificacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RevisaCicloApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    private val escopo: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onCreate() {
        super.onCreate()
        CanalNotificacao.criar(this)
        escopo.launch {
            val preferencias = container.preferenciasRepository.preferencias.first()
            container.agendadorNotificacoes.garantirAgendado(preferencias)
        }
    }
}
