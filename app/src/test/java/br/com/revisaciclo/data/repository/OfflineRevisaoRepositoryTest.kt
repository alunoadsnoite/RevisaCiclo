package br.com.revisaciclo.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.local.entity.ProjetoEstudoEntity
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.StatusRevisao
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class OfflineRevisaoRepositoryTest {
    private lateinit var database: RevisaCicloDatabase
    private lateinit var repository: OfflineRevisaoRepository
    private var disciplinaId = 0L
    private val hoje = LocalDate.of(2026, 11, 22)

    @Before fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), RevisaCicloDatabase::class.java,
        ).allowMainThreadQueries().build()
        val prefs = object : PreferenciasRepository {
            override val preferencias = MutableStateFlow(Preferencias())
            override suspend fun atualizar(transformacao: (Preferencias) -> Preferencias) {
                preferencias.value = transformacao(preferencias.value)
            }
            override suspend fun ultimaNotificacaoDia(): Long? = null
            override suspend fun registrarNotificacaoDia(epochDay: Long) = Unit
        }
        repository = OfflineRevisaoRepository(database, prefs,
            Clock.fixed(Instant.parse("2026-11-22T12:00:00Z"), ZoneOffset.UTC))
        val projeto = database.projetoEstudoDao().inserir(ProjetoEstudoEntity(
            nome = "Teste", descricao = null, cor = 0, criadoEm = 0, arquivado = false,
        ))
        disciplinaId = repository.criarDisciplina(projeto, "Português", 0)
    }

    @After fun cleanup() { database.close() }

    @Test fun editarTituloAposRevisaoPreservaAgendamento() = runBlocking {
        val id = repository.criarAssunto(disciplinaId, "Original", null, hoje.minusDays(20))
        val primeira = database.revisaoDao().buscarPendentePorAssunto(id)!!
        repository.concluirRevisao(primeira.id, AvaliacaoRevisao.BOM)
        val antes = database.revisaoDao().buscarPendentePorAssunto(id)!!
        repository.atualizarAssunto(id, disciplinaId, "Corrigido", "Descrição", hoje.minusDays(20))
        assertEquals(antes, database.revisaoDao().buscarPendentePorAssunto(id))
        val assunto = database.assuntoDao().buscarPorId(id)!!
        assertEquals("Corrigido", assunto.titulo)
        assertEquals(antes.agendadaPara, assunto.proximaRevisao)
    }

    @Test fun alterarDataDeEstudoReagendaSemDuplicarPendente() = runBlocking {
        val id = repository.criarAssunto(disciplinaId, "Tema", null, hoje.minusDays(2))
        repository.atualizarAssunto(id, disciplinaId, "Tema", null, hoje)
        val revisoes = database.revisaoDao().listarTodas()
        assertEquals(1, revisoes.size)
        assertEquals(hoje.plusDays(1).toEpochDay(), revisoes.single().agendadaPara)
    }

    @Test fun conclusoesRepetidasEConcorrentesCriamUmaUnicaProximaRevisao() = runBlocking {
        val id = repository.criarAssunto(disciplinaId, "Tema", null, hoje.minusDays(2))
        val primeira = database.revisaoDao().buscarPendentePorAssunto(id)!!
        coroutineScope {
            List(3) { async { repository.concluirRevisao(primeira.id, AvaliacaoRevisao.BOM) } }.awaitAll()
        }
        repository.concluirRevisao(primeira.id, AvaliacaoRevisao.FACIL)
        val revisoes = database.revisaoDao().listarTodas()
        assertEquals(2, revisoes.size)
        assertEquals(1, revisoes.count { it.status == StatusRevisao.PENDENTE.name })
        assertEquals(AvaliacaoRevisao.BOM.name, revisoes.first { it.id == primeira.id }.avaliacao)
    }

    @Test fun resumoRecalculaAtrasoComNovaDataSemAlterarBanco() = runBlocking {
        repository.criarAssunto(disciplinaId, "Tema", null, hoje.minusDays(1))
        assertEquals(0, repository.observarProgressoPorDisciplina(hoje).first().single().assuntosAtrasados)
        assertEquals(1, repository.observarProgressoPorDisciplina(hoje.plusDays(1)).first().single().assuntosAtrasados)
    }
}
