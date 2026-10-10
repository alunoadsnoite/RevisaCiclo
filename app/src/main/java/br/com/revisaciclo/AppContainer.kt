package br.com.revisaciclo

import android.content.Context
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.preferencias.criarPreferenciasRepository
import br.com.revisaciclo.data.repository.OfflineProjetoRepository
import br.com.revisaciclo.data.repository.OfflineRevisaoRepository
import br.com.revisaciclo.domain.notificacao.AgendadorNotificacoes
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.ProjetoRepository
import br.com.revisaciclo.domain.repository.RevisaoRepository
import br.com.revisaciclo.notificacao.NotificadorRevisoes
import br.com.revisaciclo.notificacao.WorkManagerAgendadorNotificacoes
import java.time.Clock

class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val database: RevisaCicloDatabase by lazy {
        RevisaCicloDatabase.getInstance(appContext)
    }

    val clock: Clock = Clock.systemDefaultZone()

    val preferenciasRepository: PreferenciasRepository by lazy {
        criarPreferenciasRepository(appContext)
    }

    val revisaoRepository: RevisaoRepository by lazy {
        OfflineRevisaoRepository(database, preferenciasRepository, clock)
    }

    val projetoRepository: ProjetoRepository by lazy {
        OfflineProjetoRepository(database, preferenciasRepository, clock)
    }

    val agendadorNotificacoes: AgendadorNotificacoes by lazy {
        WorkManagerAgendadorNotificacoes(appContext, clock)
    }

    val notificadorRevisoes: NotificadorRevisoes by lazy {
        NotificadorRevisoes(appContext)
    }
}
