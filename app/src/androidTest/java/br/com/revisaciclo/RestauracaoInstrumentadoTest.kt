package br.com.revisaciclo

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.revisaciclo.data.backup.ArmazenamentoBackupRoom
import br.com.revisaciclo.data.backup.ArmazenamentoSegurancaArquivos
import br.com.revisaciclo.data.backup.EstadoOperacaoDataStore
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.preferencias.DataStorePreferenciasRepository
import br.com.revisaciclo.domain.backup.BackupDados
import br.com.revisaciclo.domain.backup.EstadoOperacao
import br.com.revisaciclo.domain.backup.ReconciliadorRestauracao
import br.com.revisaciclo.domain.backup.ResultadoRestauracao
import br.com.revisaciclo.domain.backup.SerializadorBackup
import br.com.revisaciclo.domain.backup.ServicoRestauracao
import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import br.com.revisaciclo.domain.notificacao.GerenciadorNotificacoes
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Restauração ponta a ponta sobre Room + DataStore reais e reconciliação após
 * interrupção. Requer dispositivo/emulador.
 */
@RunWith(AndroidJUnit4::class)
class RestauracaoInstrumentadoTest {

    private lateinit var contexto: Context
    private lateinit var banco: RevisaCicloDatabase
    private lateinit var escopo: CoroutineScope
    private lateinit var preferencias: DataStorePreferenciasRepository
    private lateinit var estadoOperacao: EstadoOperacaoDataStore
    private lateinit var seguranca: ArmazenamentoSegurancaArquivos
    private lateinit var diretorioSeguranca: File
    private lateinit var armazenamento: ArmazenamentoBackupRoom
    private lateinit var agendador: AgendadorFake
    private lateinit var gerenciador: GerenciadorNotificacoes

    @Before
    fun preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().targetContext
        banco = Room.inMemoryDatabaseBuilder(contexto, RevisaCicloDatabase::class.java).build()
        escopo = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val sufixo = System.nanoTime()
        val dsPreferencias = PreferenceDataStoreFactory.create(scope = escopo) {
            File(contexto.cacheDir, "restore_prefs_$sufixo.preferences_pb")
        }
        val dsEstado = PreferenceDataStoreFactory.create(scope = escopo) {
            File(contexto.cacheDir, "restore_estado_$sufixo.preferences_pb")
        }
        preferencias = DataStorePreferenciasRepository(dsPreferencias)
        estadoOperacao = EstadoOperacaoDataStore(dsEstado)
        diretorioSeguranca = File(contexto.cacheDir, "seg_$sufixo")
        seguranca = ArmazenamentoSegurancaArquivos(diretorioSeguranca)
        armazenamento = ArmazenamentoBackupRoom(banco, preferencias)
        agendador = AgendadorFake()
        gerenciador = GerenciadorNotificacoes(agendador)
    }

    @After
    fun limpar() {
        banco.close()
        escopo.cancel()
    }

    @Test
    fun restauracaoSubstituiBancoPopuladoEAtualizaPreferencias() = runBlocking {
        armazenamento.substituirRoom(dados("Antigo"))
        preferencias.atualizar { it.copy(metaDiaria = 5) }

        val servico = servicoCom(FonteArquivoFake())
        val novos = dados("Novo", metaDiaria = 42)
        val doc = SerializadorBackup.desserializar(bytesDe(novos))

        val resultado = servico.restaurar(doc)

        assertTrue(resultado is ResultadoRestauracao.Sucesso)
        assertEquals("Novo", banco.projetoEstudoDao().listarTodos().single().nome)
        assertEquals(42, preferencias.preferencias.first().metaDiaria)
        assertNull(estadoOperacao.ler())
        assertEquals(1, agendador.reagendamentos)
        assertTrue(diretorioSeguranca.listFiles()?.isEmpty() ?: true)
    }

    @Test
    fun reconciliacaoConcluiPreferenciasAposInterrupcao() = runBlocking {
        // Simula interrupção: Room já substituído e marcador gravado, mas as
        // preferências antigas ainda estão no DataStore.
        armazenamento.substituirRoom(dados("Novo"))
        preferencias.atualizar { it.copy(metaDiaria = 5) }
        estadoOperacao.gravar(EstadoOperacao(Preferencias(metaDiaria = 42)))

        val reconciliador = ReconciliadorRestauracao(estadoOperacao, armazenamento, gerenciador)
        val concluiu = reconciliador.reconciliar()

        assertTrue(concluiu)
        assertEquals(42, preferencias.preferencias.first().metaDiaria)
        assertNull(estadoOperacao.ler())
        assertEquals(1, agendador.reagendamentos)
    }

    @Test
    fun lerArquivoBackupValidoRetornaPronto() = runBlocking {
        val fonte = FonteArquivoFake(conteudo = bytesDe(dados("Novo")))
        val resultado = servicoCom(fonte).lerArquivo("content://teste")

        assertTrue(resultado is br.com.revisaciclo.domain.backup.ResultadoLeituraBackup.Pronto)
    }

    private fun servicoCom(fonte: FonteArquivoFake) = ServicoRestauracao(
        armazenamento = armazenamento,
        estadoOperacao = estadoOperacao,
        fonteArquivo = fonte,
        armazenamentoSeguranca = seguranca,
        gerenciadorNotificacoes = gerenciador,
        versaoApp = "0.1.0",
        clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), java.time.ZoneOffset.UTC),
    )

    private fun bytesDe(dados: BackupDados): ByteArray =
        SerializadorBackup.serializarBytes(
            dados,
            "0.1.0",
            Instant.parse("2026-10-10T10:00:00Z"),
        )

    private fun dados(nomeProjeto: String, metaDiaria: Int = 20) = BackupDados(
        projetos = listOf(ProjetoEstudo(id = 7L, nome = nomeProjeto, cor = 0, criadoEm = 7L)),
        disciplinas = listOf(Disciplina(id = 8L, projetoId = 7L, nome = "D", cor = 0, criadaEm = 8L)),
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
            Revisao(
                id = 10L,
                assuntoId = 9L,
                agendadaPara = LocalDate.of(2026, 1, 2),
                concluidaEm = null,
                status = StatusRevisao.PENDENTE,
                intervaloDias = 1,
                indiceCiclo = 0,
                avaliacao = AvaliacaoRevisao.BOM,
            ),
        ),
        preferencias = Preferencias(metaDiaria = metaDiaria, projetoAtivoId = 7L),
    )
}
