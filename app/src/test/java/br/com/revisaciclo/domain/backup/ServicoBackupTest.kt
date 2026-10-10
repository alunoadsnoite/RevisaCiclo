package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.ProjetoEstudo
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicoBackupTest {

    private val dados = BackupDados(
        projetos = listOf(ProjetoEstudo(id = 1L, nome = "P", cor = 0, criadoEm = 1L)),
        disciplinas = listOf(Disciplina(id = 2L, projetoId = 1L, nome = "D", cor = 0, criadaEm = 2L)),
        assuntos = listOf(
            Assunto(
                id = 3L,
                disciplinaId = 2L,
                titulo = "A",
                descricao = null,
                dataEstudo = LocalDate.of(2026, 1, 1),
                estadoRevisao = EstadoRevisao.NOVO,
                indiceCiclo = 0,
                proximaRevisao = null,
            ),
        ),
        revisoes = emptyList(),
        preferencias = Preferencias(metaDiaria = 12),
    )

    private val clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `criar backup gera documento valido com dados atuais`() = runBlocking {
        val servico = ServicoBackup(
            armazenamento = ArmazenamentoBackupFake(dados = dados),
            fonteArquivo = FonteArquivoFake(),
            versaoApp = "0.1.0",
            clock = clock,
        )

        val doc = SerializadorBackup.desserializar(servico.criarBackupBytes())

        assertEquals(1, doc.dados.projetos.size)
        assertEquals(1, doc.dados.assuntos.size)
        assertEquals(12, doc.dados.preferencias.metaDiaria)
        assertEquals(Instant.parse("2026-10-10T10:00:00Z"), doc.cabecalho.criadoEm)
    }

    @Test
    fun `salvar backup escreve no destino escolhido`() = runBlocking {
        val fonte = FonteArquivoFake()
        val servico = ServicoBackup(ArmazenamentoBackupFake(dados = dados), fonte, "0.1.0", clock)

        servico.salvarBackup("content://destino/backup")

        assertEquals("content://destino/backup", fonte.ultimoIdentificadorEscrito)
        assertNotNull(fonte.escrito)
        assertTrue(fonte.escrito!!.isNotEmpty())
    }

    @Test
    fun `exportar json escreve json portatil`() = runBlocking {
        val fonte = FonteArquivoFake()
        val servico = ServicoBackup(ArmazenamentoBackupFake(dados = dados), fonte, "0.1.0", clock)

        servico.salvarJson("content://destino/export.json")

        val texto = String(fonte.escrito!!, Charsets.UTF_8)
        assertTrue(texto.contains("\"schemaVersion\""))
        assertTrue(texto.contains("\"projects\""))
        assertTrue(texto.contains("\"topics\""))
    }
}
