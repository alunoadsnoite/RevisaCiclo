package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.notificacao.AgendadorNotificacoes
import br.com.revisaciclo.domain.notificacao.GerenciadorNotificacoes
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicoRestauracaoTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneOffset.UTC)

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
        preferencias = Preferencias(metaDiaria = 33),
    )

    private class AgendadorFake : AgendadorNotificacoes {
        var reagendamentos = 0
        var cancelamentos = 0
        var ultima: Preferencias? = null
        override fun garantirAgendado(preferencias: Preferencias) {}
        override fun reagendar(preferencias: Preferencias) {
            reagendamentos++
            ultima = preferencias
        }
        override fun cancelar() { cancelamentos++ }
    }

    private fun servico(
        armazenamento: ArmazenamentoBackupFake = ArmazenamentoBackupFake(),
        estado: EstadoOperacaoFake = EstadoOperacaoFake(),
        fonte: FonteArquivoFake = FonteArquivoFake(),
        seguranca: ArmazenamentoSegurancaFake = ArmazenamentoSegurancaFake(),
        agendador: AgendadorFake = AgendadorFake(),
    ) = ServicoRestauracao(
        armazenamento = armazenamento,
        estadoOperacao = estado,
        fonteArquivo = fonte,
        armazenamentoSeguranca = seguranca,
        gerenciadorNotificacoes = GerenciadorNotificacoes(agendador),
        versaoApp = "0.1.0",
        clock = clock,
    )

    private fun bytesValidos(dados: BackupDados): ByteArray =
        SerializadorBackup.serializarBytes(dados, "0.1.0", Instant.now(clock))

    @Test
    fun `restauracao bem sucedida aplica dados e preferencias e limpa marcador`() = runBlocking {
        val armazenamento = ArmazenamentoBackupFake()
        val estado = EstadoOperacaoFake()
        val seguranca = ArmazenamentoSegurancaFake()
        val agendador = AgendadorFake()

        val resultado = servico(armazenamento, estado, seguranca = seguranca, agendador = agendador)
            .restaurar(documento(dados))

        assertTrue(resultado is ResultadoRestauracao.Sucesso)
        assertEquals(1, armazenamento.substituicoes)
        assertEquals(33, armazenamento.prefsAplicadas?.metaDiaria)
        assertEquals(1, estado.gravacoes)
        assertEquals(1, estado.limpezas)
        assertNull(estado.estado)
        assertNotNull(seguranca.salvo)
        assertNotNull(seguranca.apagado)
        assertEquals(1, agendador.reagendamentos)
    }

    @Test
    fun `falha ao substituir banco nao aplica preferencias e apaga snapshot`() = runBlocking {
        val armazenamento = ArmazenamentoBackupFake(falharSubstituir = IOException("boom"))
        val estado = EstadoOperacaoFake()
        val seguranca = ArmazenamentoSegurancaFake()

        val resultado = servico(armazenamento, estado, seguranca = seguranca).restaurar(documento(dados))

        assertTrue(resultado is ResultadoRestauracao.Falha)
        assertEquals(TipoFalhaRestauracao.ESCRITA, (resultado as ResultadoRestauracao.Falha).tipo)
        assertTrue(!resultado.copiaSegurancaMantida)
        assertEquals(0, estado.gravacoes)
        assertNull(armazenamento.prefsAplicadas)
        assertNotNull(seguranca.apagado)
    }

    @Test
    fun `falha ao aplicar preferencias mantem marcador e snapshot`() = runBlocking {
        val armazenamento = ArmazenamentoBackupFake(falharAplicar = IOException("boom"))
        val estado = EstadoOperacaoFake()
        val seguranca = ArmazenamentoSegurancaFake()

        val resultado = servico(armazenamento, estado, seguranca = seguranca).restaurar(documento(dados))

        assertTrue(resultado is ResultadoRestauracao.Falha)
        assertTrue((resultado as ResultadoRestauracao.Falha).copiaSegurancaMantida)
        assertEquals(1, estado.gravacoes)
        assertEquals(0, estado.limpezas)
        assertNotNull(estado.estado)
        assertNull(seguranca.apagado)
    }

    @Test
    fun `cancelamento durante substituicao e propagado`() {
        val armazenamento = ArmazenamentoBackupFake(
            falharSubstituir = CancellationException("cancelado"),
        )
        val servico = servico(armazenamento)

        try {
            runBlocking { servico.restaurar(documento(dados)) }
            throw AssertionError("Esperava CancellationException")
        } catch (esperado: CancellationException) {
            // ok: propagado
        }
    }

    @Test
    fun `ler arquivo grande retorna invalido`() = runBlocking {
        val fonte = FonteArquivoFake(falha = ArquivoGrandeDemaisException())
        val resultado = servico(fonte = fonte).lerArquivo("content://grande")

        assertTrue(resultado is ResultadoLeituraBackup.Invalido)
        assertEquals(
            TipoFalhaRestauracao.ARQUIVO_INVALIDO,
            (resultado as ResultadoLeituraBackup.Invalido).tipo,
        )
    }

    @Test
    fun `ler arquivo inexistente retorna erro de leitura`() = runBlocking {
        val fonte = FonteArquivoFake(falha = IOException("não existe"))
        val resultado = servico(fonte = fonte).lerArquivo("content://x")

        assertTrue(resultado is ResultadoLeituraBackup.Invalido)
        assertEquals(
            TipoFalhaRestauracao.LEITURA,
            (resultado as ResultadoLeituraBackup.Invalido).tipo,
        )
    }

    @Test
    fun `ler backup valido retorna previa com resumo`() = runBlocking {
        val fonte = FonteArquivoFake(conteudo = bytesValidos(dados))
        val resultado = servico(fonte = fonte).lerArquivo("content://ok")

        assertTrue(resultado is ResultadoLeituraBackup.Pronto)
        val pronto = resultado as ResultadoLeituraBackup.Pronto
        assertEquals(1, pronto.resumo.projetos)
        assertEquals(1, pronto.resumo.assuntos)
    }

    @Test
    fun `ler arquivo com dados invalidos retorna invalido`() = runBlocking {
        val invalidos = dados.copy(
            disciplinas = listOf(Disciplina(id = 2L, projetoId = 999L, nome = "D", cor = 0, criadaEm = 2L)),
        )
        val fonte = FonteArquivoFake(conteudo = bytesValidos(invalidos))
        val resultado = servico(fonte = fonte).lerArquivo("content://invalido")

        assertTrue(resultado is ResultadoLeituraBackup.Invalido)
        assertEquals(
            TipoFalhaRestauracao.DADOS_INVALIDOS,
            (resultado as ResultadoLeituraBackup.Invalido).tipo,
        )
    }

    private fun documento(dados: BackupDados): BackupDocumento =
        SerializadorBackup.desserializar(bytesValidos(dados))
}
