package br.com.revisaciclo.data.preferencias

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.TemaApp
import br.com.revisaciclo.domain.scheduler.MotorRevisaoEspacada
import java.io.File
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStorePreferenciasRepositoryTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private val escopo = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() {
        escopo.cancel()
    }

    private fun criarRepositorio(): DataStorePreferenciasRepository {
        val arquivo = File(pasta.root, "prefs_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = escopo,
            produceFile = { arquivo },
        )
        return DataStorePreferenciasRepository(dataStore)
    }

    @Test
    fun `usa valores padrao quando ainda nao ha nada salvo`() = runBlocking {
        val repositorio = criarRepositorio()

        assertEquals(Preferencias(), repositorio.preferencias.first())
    }

    @Test
    fun `persiste e le todas as preferencias`() = runBlocking {
        val repositorio = criarRepositorio()

        repositorio.atualizar {
            it.copy(
                notificacoesAtivas = false,
                horarioNotificacao = LocalTime.of(21, 30),
                metaDiaria = 42,
                tema = TemaApp.ESCURO,
                intervalosDias = listOf(1, 2, 4, 8),
            )
        }

        val preferencias = repositorio.preferencias.first()
        assertEquals(false, preferencias.notificacoesAtivas)
        assertEquals(LocalTime.of(21, 30), preferencias.horarioNotificacao)
        assertEquals(42, preferencias.metaDiaria)
        assertEquals(TemaApp.ESCURO, preferencias.tema)
        assertEquals(listOf(1, 2, 4, 8), preferencias.intervalosDias)
    }

    @Test
    fun `atualizacao parcial preserva os demais campos`() = runBlocking {
        val repositorio = criarRepositorio()

        repositorio.atualizar { it.copy(metaDiaria = 10) }
        repositorio.atualizar { it.copy(tema = TemaApp.CLARO) }

        val preferencias = repositorio.preferencias.first()
        assertEquals(10, preferencias.metaDiaria)
        assertEquals(TemaApp.CLARO, preferencias.tema)
        assertEquals(Preferencias().notificacoesAtivas, preferencias.notificacoesAtivas)
    }

    @Test
    fun `registra e le o dia da ultima notificacao`() = runBlocking {
        val repositorio = criarRepositorio()

        assertNull(repositorio.ultimaNotificacaoDia())
        repositorio.registrarNotificacaoDia(20_000L)

        assertEquals(20_000L, repositorio.ultimaNotificacaoDia())
    }

    @Test
    fun `persiste o projeto ativo e o limpa quando nulo`() = runBlocking {
        val repositorio = criarRepositorio()

        assertNull(repositorio.preferencias.first().projetoAtivoId)
        repositorio.atualizar { it.copy(projetoAtivoId = 7L) }
        assertEquals(7L, repositorio.preferencias.first().projetoAtivoId)

        repositorio.atualizar { it.copy(projetoAtivoId = null) }
        assertNull(repositorio.preferencias.first().projetoAtivoId)
    }

    @Test
    fun `mapping de preferencias vazias devolve os padroes`() {
        assertEquals(Preferencias(), emptyPreferences().toPreferencias())
    }

    @Test
    fun `decodificarIntervalos aceita lista valida e cai no padrao quando invalida`() {
        assertEquals(listOf(1, 5, 10), decodificarIntervalos("1,5,10"))
        assertEquals(MotorRevisaoEspacada.INTERVALOS_PADRAO, decodificarIntervalos("abc"))
        assertEquals(MotorRevisaoEspacada.INTERVALOS_PADRAO, decodificarIntervalos(""))
    }
}
