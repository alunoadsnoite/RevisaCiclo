package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorBackupTest {

    private val base = BackupDados(
        projetos = listOf(
            ProjetoEstudo(id = 1L, nome = "P", cor = 0, criadoEm = 1L),
        ),
        disciplinas = listOf(
            Disciplina(id = 10L, projetoId = 1L, nome = "D", cor = 0, criadaEm = 1L),
        ),
        assuntos = listOf(
            Assunto(
                id = 100L,
                disciplinaId = 10L,
                titulo = "A",
                descricao = null,
                dataEstudo = LocalDate.of(2026, 1, 1),
                estadoRevisao = EstadoRevisao.NOVO,
                indiceCiclo = 0,
                proximaRevisao = null,
            ),
        ),
        revisoes = listOf(
            Revisao(
                id = 1000L,
                assuntoId = 100L,
                agendadaPara = LocalDate.of(2026, 1, 1),
                concluidaEm = null,
                status = StatusRevisao.PENDENTE,
                intervaloDias = 1,
                indiceCiclo = 0,
                avaliacao = null,
            ),
        ),
        preferencias = Preferencias(),
    )

    private fun doc(dados: BackupDados) = BackupDocumento(
        cabecalho = BackupCabecalho(
            formato = BackupFormato.IDENTIFICADOR,
            versaoFormato = BackupFormato.VERSAO,
            versaoApp = "0.1.0",
            versaoSchemaBanco = BackupFormato.VERSAO_SCHEMA_BANCO,
            criadoEm = Instant.EPOCH,
            algoritmoChecksum = "SHA-256",
            checksum = "a".repeat(64),
            contagens = BackupContagens(
                projetos = dados.projetos.size,
                disciplinas = dados.disciplinas.size,
                assuntos = dados.assuntos.size,
                revisoes = dados.revisoes.size,
            ),
        ),
        dados = dados,
    )

    @Test
    fun `valido quando consistente`() {
        val r = ValidadorBackup.validar(doc(base))
        assertTrue(r is ResultadoValidacaoBackup.Valido)
    }

    @Test
    fun `invalido quando contagens inconsistentes`() {
        val d = base.copy(projetos = emptyList())
        val cab = BackupCabecalho(
            formato = BackupFormato.IDENTIFICADOR,
            versaoFormato = BackupFormato.VERSAO,
            versaoApp = "0.1.0",
            versaoSchemaBanco = BackupFormato.VERSAO_SCHEMA_BANCO,
            criadoEm = Instant.EPOCH,
            algoritmoChecksum = "SHA-256",
            checksum = "a".repeat(64),
            contagens = BackupContagens(
                projetos = 99,
                disciplinas = 0,
                assuntos = 0,
                revisoes = 0,
            ),
        )
        val r = ValidadorBackup.validar(BackupDocumento(cabecalho = cab, dados = d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.CONTAGENS_INCONSISTENTES, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando ids duplicados`() {
        val d = base.copy(
            projetos = listOf(
                ProjetoEstudo(id = 1L, nome = "P1", cor = 0, criadoEm = 1L),
                ProjetoEstudo(id = 1L, nome = "P2", cor = 0, criadoEm = 2L),
            ),
        )
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.IDS_DUPLICADOS, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando id nao positivo`() {
        val d = base.copy(projetos = listOf(ProjetoEstudo(id = 0L, nome = "P", cor = 0, criadoEm = 1L)))
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.ID_INVALIDO, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando disciplina referencia projeto inexistente`() {
        val d = base.copy(disciplinas = listOf(Disciplina(id = 10L, projetoId = 999L, nome = "D", cor = 0, criadaEm = 1L)))
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.RELACAO_INVALIDA, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando assunto referencia disciplina inexistente`() {
        val d = base.copy(
            assuntos = listOf(
                Assunto(
                    id = 100L,
                    disciplinaId = 999L,
                    titulo = "A",
                    descricao = null,
                    dataEstudo = LocalDate.of(2026, 1, 1),
                    estadoRevisao = EstadoRevisao.NOVO,
                    indiceCiclo = 0,
                    proximaRevisao = null,
                ),
            ),
        )
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.RELACAO_INVALIDA, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando revisao referencia assunto inexistente`() {
        val d = base.copy(
            revisoes = listOf(
                Revisao(
                    id = 1000L,
                    assuntoId = 999L,
                    agendadaPara = LocalDate.of(2026, 1, 1),
                    concluidaEm = null,
                    status = StatusRevisao.PENDENTE,
                    intervaloDias = 1,
                    indiceCiclo = 0,
                    avaliacao = null,
                ),
            ),
        )
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.RELACAO_INVALIDA, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando projeto sem nome`() {
        val d = base.copy(projetos = listOf(ProjetoEstudo(id = 1L, nome = "", cor = 0, criadoEm = 1L)))
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.CAMPO_OBRIGATORIO, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando assunto sem titulo`() {
        val d = base.copy(
            assuntos = listOf(
                Assunto(
                    id = 100L,
                    disciplinaId = 10L,
                    titulo = "",
                    descricao = null,
                    dataEstudo = LocalDate.of(2026, 1, 1),
                    estadoRevisao = EstadoRevisao.NOVO,
                    indiceCiclo = 0,
                    proximaRevisao = null,
                ),
            ),
        )
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.CAMPO_OBRIGATORIO, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando meta diaria fora do intervalo`() {
        val d = base.copy(preferencias = Preferencias(metaDiaria = 0))
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.PREFERENCIAS_INVALIDAS, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando intervalo dias vazio`() {
        val d = base.copy(preferencias = Preferencias(intervalosDias = emptyList()))
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.PREFERENCIAS_INVALIDAS, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }

    @Test
    fun `invalido quando excede limite de registros`() {
        val muitos = (1..(BackupFormato.LIMITE_REGISTROS + 1)).map { i ->
            ProjetoEstudo(id = i.toLong(), nome = "P$i", cor = 0, criadoEm = 1L)
        }
        val d = base.copy(projetos = muitos)
        val r = ValidadorBackup.validar(doc(d))
        assertTrue(r is ResultadoValidacaoBackup.Invalido)
        assertEquals(MotivoValidacaoBackup.LIMITE_EXCEDIDO, (r as ResultadoValidacaoBackup.Invalido).motivo)
    }
}
