package br.com.revisaciclo

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.revisaciclo.notificacao.CanalNotificacao
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InicializacaoInstrumentadaTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val app: RevisaCicloApp
        get() = contexto.applicationContext as RevisaCicloApp

    @Test
    fun inicializacaoCriaCanalDeNotificacao() {
        val gerenciador = contexto.getSystemService(NotificationManager::class.java)

        assertNotNull(gerenciador.getNotificationChannel(CanalNotificacao.ID))
    }

    @Test
    fun containerInicializaRepositoriosEServicos() = runBlocking {
        val container = app.container

        assertEquals("0.1.0", container.versaoApp)

        val preferencias = container.preferenciasRepository.preferencias.first()
        assertNotNull(preferencias)

        assertNotNull(container.revisaoRepository.observarAssuntos().first())
        assertNotNull(container.projetoRepository.observarProjetos().first())
        assertNotNull(LocalDate.now(container.clock))

        assertNotNull(container.servicoBackup)
        assertNotNull(container.servicoRestauracao)
        assertNotNull(container.reconciliadorRestauracao)
        assertNotNull(container.publicadorArquivo)
        assertNotNull(container.notificadorRevisoes)
    }
}
