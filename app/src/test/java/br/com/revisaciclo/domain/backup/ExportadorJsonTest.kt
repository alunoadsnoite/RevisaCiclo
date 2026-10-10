package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ExportadorJsonTest {

    private val dados = BackupDados(
        projetos = listOf(
            ProjetoEstudo(id = 1L, nome = "P", cor = 0x111111, criadoEm = 1L, arquivado = false),
        ),
        disciplinas = listOf(
            Disciplina(id = 2L, projetoId = 1L, nome = "D", cor = 0x222222, criadaEm = 2L),
        ),
        assuntos = listOf(
            Assunto(
                id = 3L,
                disciplinaId = 2L,
                titulo = "A",
                descricao = "desc",
                dataEstudo = LocalDate.of(2026, 1, 1),
                estadoRevisao = EstadoRevisao.EM_DIA,
                indiceCiclo = 1,
                proximaRevisao = LocalDate.of(2026, 1, 3),
            ),
        ),
        revisoes = listOf(
            Revisao(
                id = 4L,
                assuntoId = 3L,
                agendadaPara = LocalDate.of(2026, 1, 3),
                concluidaEm = LocalDate.of(2026, 1, 3),
                status = StatusRevisao.CONCLUIDA,
                intervaloDias = 2,
                indiceCiclo = 1,
                avaliacao = AvaliacaoRevisao.BOM,
            ),
        ),
        preferencias = Preferencias(
            notificacoesAtivas = false,
            metaDiaria = 5,
        ),
    )

    @Test
    fun `exportacao json possui schema e chaves em ingles`() {
        val agora = Instant.parse("2026-10-10T12:00:00Z")
        val json = ExportadorJson.exportar(dados, "0.1.0", agora)
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"exportedAt\": \"2026-10-10T12:00:00Z\""))
        assertTrue(json.contains("\"appVersion\": \"0.1.0\""))
        assertTrue(json.contains("\"projects\""))
        assertTrue(json.contains("\"disciplines\""))
        assertTrue(json.contains("\"topics\""))
        assertTrue(json.contains("\"reviews\""))
        assertTrue(json.contains("\"preferences\""))
    }

    @Test
    fun `exportacao e legivel`() {
        val agora = Instant.parse("2026-10-10T12:00:00Z")
        val json = ExportadorJson.exportar(dados, "0.1.0", agora)
        assertTrue(json.contains("{\n"))
        assertTrue(json.contains("\n}"))
    }
}
