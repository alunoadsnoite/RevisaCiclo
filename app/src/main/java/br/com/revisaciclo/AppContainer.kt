package br.com.revisaciclo

import android.content.Context
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.repository.OfflineRevisaoRepository
import br.com.revisaciclo.domain.repository.RevisaoRepository
import java.time.Clock

class AppContainer(context: Context) {
    private val database: RevisaCicloDatabase by lazy {
        RevisaCicloDatabase.getInstance(context)
    }

    val clock: Clock = Clock.systemDefaultZone()

    val revisaoRepository: RevisaoRepository by lazy {
        OfflineRevisaoRepository(database, clock)
    }
}
