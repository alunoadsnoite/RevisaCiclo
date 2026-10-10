package br.com.revisaciclo

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.revisaciclo.data.backup.ArmazenamentoBackupRoom
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.domain.backup.BackupDados
import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.ProjetoEstudo
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persistência real em Room: captura de snapshot, substituição atômica e
 * preservação de IDs em banco já populado.
 *
 * Requer dispositivo/emulador; executa via `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ArmazenamentoBackupInstrumentadoTest {

    private lateinit var contexto: Context
    private lateinit var banco: RevisaCicloDatabase
    private lateinit var preferencias: PreferenciasRepositoryFake
    private lateinit var armazenamento: ArmazenamentoBackupRoom

    @Before
    fun preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().targetContext
        banco = Room.inMemoryDatabaseBuilder(contexto, RevisaCicloDatabase::class.java).build()
        preferencias = PreferenciasRepositoryFake()
        armazenamento = ArmazenamentoBackupRoom(banco, preferencias)
    }

    @After
    fun limpar() {
        banco.close()
    }

    @Test
    fun substituicaoPreservaIdsEHistóricoEmBancoPopulado() = runBlocking {
        armazenamento.substituirRoom(dadosAntigos())

        val novos = dadosNovos()
        armazenamento.substituirRoom(novos)

        val projetos = banco.projetoEstudoDao().listarTodos()
        assertEquals(listOf(7L), projetos.map { it.id })
        assertEquals("Novo Projeto", projetos.single().nome)

        assertEquals(listOf(8L), banco.disciplinaDao().listarTodas().map { it.id })
        assertEquals(listOf(9L), banco.assuntoDao().listarTodos().map { it.id })
        assertEquals(listOf(10L), banco.revisaoDao().listarTodas().map { it.id })

        val revisao = banco.revisaoDao().listarTodas().single()
        assertEquals(9L, revisao.assuntoId)
        assertTrue(revisao.avaliacao != null)
    }

    @Test
    fun capturaLeSnapshotCompletoEPreferencias() = runBlocking {
        preferencias.atualizar { it.copy(metaDiaria = 42) }
        armazenamento.substituirRoom(dadosNovos())

        val capturado = armazenamento.capturar()

        assertEquals(1, capturado.projetos.size)
        assertEquals(1, capturado.disciplinas.size)
        assertEquals(1, capturado.assuntos.size)
        assertEquals(1, capturado.revisoes.size)
        assertEquals(42, capturado.preferencias.metaDiaria)
    }

    @Test
    fun substituicaoComListaVaziaLimpaTabelas() = runBlocking {
        armazenamento.substituirRoom(dadosNovos())

        armazenamento.substituirRoom(
            BackupDados(emptyList(), emptyList(), emptyList(), emptyList(), Preferencias()),
        )

        assertEquals(0, banco.projetoEstudoDao().contar())
        assertEquals(0, banco.disciplinaDao().contar())
        assertEquals(0, banco.assuntoDao().contar())
        assertEquals(0, banco.revisaoDao().contar())
    }

    private fun dadosAntigos() = BackupDados(
        projetos = listOf(ProjetoEstudo(id = 1L, nome = "Antigo", cor = 0, criadoEm = 1L)),
        disciplinas = listOf(Disciplina(id = 2L, projetoId = 1L, nome = "D1", cor = 0, criadaEm = 2L)),
        assuntos = emptyList(),
        revisoes = emptyList(),
        preferencias = Preferencias(),
    )

    private fun dadosNovos() = BackupDados(
        projetos = listOf(ProjetoEstudo(id = 7L, nome = "Novo Projeto", cor = 0, criadoEm = 7L)),
        disciplinas = listOf(Disciplina(id = 8L, projetoId = 7L, nome = "D2", cor = 0, criadaEm = 8L)),
        assuntos = listOf(
            Assunto(
                id = 9L,
                disciplinaId = 8L,
                titulo = "A",
                descricao = null,
                dataEstudo = LocalDate.of(2026, 1, 1),
                estadoRevisao = EstadoRevisao.NOVO,
                indiceCiclo = 0,
                proximaRevisao = LocalDate.of(2026, 1, 2),
            ),
        ),
        revisoes = listOf(
            br.com.revisaciclo.domain.model.Revisao(
                id = 10L,
                assuntoId = 9L,
                agendadaPara = LocalDate.of(2026, 1, 2),
                concluidaEm = null,
                status = br.com.revisaciclo.domain.model.StatusRevisao.PENDENTE,
                intervaloDias = 1,
                indiceCiclo = 0,
                avaliacao = br.com.revisaciclo.domain.model.AvaliacaoRevisao.BOM,
            ),
        ),
        preferencias = Preferencias(),
    )
}
