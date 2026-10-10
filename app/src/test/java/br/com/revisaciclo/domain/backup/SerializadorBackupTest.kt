package br.com.revisaciclo.domain.backup

import java.time.Instant
import java.time.LocalDate
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

class SerializadorBackupTest {

    private val dados = BackupDados(
        projetos = listOf(
            ProjetoEstudo(id = 1L, nome = "P", descricao = null, cor = 0xFF0000, criadoEm = 1000L),
        ),
        disciplinas = listOf(
            Disciplina(id = 1L, projetoId = 1L, nome = "D", cor = 0x00FF00, criadaEm = 2000L),
        ),
        assuntos = listOf(
            Assunto(
                id = 1L,
                disciplinaId = 1L,
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
                id = 1L,
                assuntoId = 1L,
                agendadaPara = LocalDate.of(2026, 1, 2),
                concluidaEm = null,
                status = StatusRevisao.PENDENTE,
                intervaloDias = 1,
                indiceCiclo = 0,
                avaliacao = null,
            ),
        ),
        preferencias = Preferencias(),
    )

    @Test
    fun `roundtrip preserva contagens e dados`() {
        val agora = Instant.parse("2026-10-10T10:00:00Z")
        val bytes = SerializadorBackup.serializarBytes(dados, "0.1.0", agora)
        val doc = SerializadorBackup.desserializar(bytes)
        assertEquals(1, doc.dados.projetos.size)
        assertEquals(1, doc.dados.disciplinas.size)
        assertEquals(1, doc.dados.assuntos.size)
        assertEquals(1, doc.dados.revisoes.size)
        assertEquals(BackupFormato.IDENTIFICADOR, doc.cabecalho.formato)
        assertEquals(BackupFormato.VERSAO, doc.cabecalho.versaoFormato)
    }

    @Test
    fun `checksum é hex de sha256`() {
        val agora = Instant.parse("2026-10-10T10:00:00Z")
        val bytes = SerializadorBackup.serializarBytes(dados, "0.1.0", agora)
        val doc = SerializadorBackup.desserializar(bytes)
        assertEquals("SHA-256", doc.cabecalho.algoritmoChecksum)
        assertEquals(64, doc.cabecalho.checksum.length)
        doc.cabecalho.checksum.forEach { c -> assertTrue(c.isDigit() || (c >= 'a' && c <= 'f')) }
    }
}
