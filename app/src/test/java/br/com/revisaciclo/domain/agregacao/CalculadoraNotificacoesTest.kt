package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraNotificacoesTest {

    private val hoje = LocalDate.of(2026, 1, 10)

    private fun projeto(id: Long, arquivado: Boolean = false) = ProjetoEstudo(
        id = id,
        nome = "P$id",
        descricao = null,
        cor = 0xFF1565C0,
        criadoEm = id,
        arquivado = arquivado,
    )

    private fun disciplina(id: Long, projetoId: Long) = Disciplina(
        id = id,
        projetoId = projetoId,
        nome = "D$id",
        cor = 0xFF1565C0,
        criadaEm = 1L,
    )

    private fun assunto(id: Long, disciplinaId: Long) = Assunto(
        id = id,
        disciplinaId = disciplinaId,
        titulo = "A$id",
        descricao = null,
        dataEstudo = LocalDate.of(2026, 1, 1),
        estadoRevisao = EstadoRevisao.EM_DIA,
        indiceCiclo = 1,
        proximaRevisao = hoje,
    )

    private fun revisao(
        id: Long,
        assuntoId: Long,
        agendadaPara: LocalDate,
        status: StatusRevisao = StatusRevisao.PENDENTE,
    ) = Revisao(
        id = id,
        assuntoId = assuntoId,
        agendadaPara = agendadaPara,
        concluidaEm = null,
        status = status,
        intervaloDias = 1,
        indiceCiclo = 1,
        avaliacao = null,
    )

    @Test
    fun `conta devidas vencendo hoje e atrasadas`() {
        val projetos = listOf(projeto(1))
        val disciplinas = listOf(disciplina(1, 1))
        val assuntos = listOf(assunto(1, 1))
        val revisoes = listOf(
            revisao(1, 1, hoje),
            revisao(2, 1, hoje.minusDays(1)),
            revisao(3, 1, hoje.plusDays(1)),
            revisao(4, 1, hoje, status = StatusRevisao.CONCLUIDA),
        )

        val total = CalculadoraNotificacoes.contarDevidas(revisoes, assuntos, disciplinas, projetos, hoje)

        assertEquals(2, total)
    }

    @Test
    fun `ignora revisoes de projetos arquivados`() {
        val projetos = listOf(projeto(1), projeto(2, arquivado = true))
        val disciplinas = listOf(disciplina(1, 1), disciplina(2, 2))
        val assuntos = listOf(assunto(1, 1), assunto(2, 2))
        val revisoes = listOf(
            revisao(1, 1, hoje),
            revisao(2, 2, hoje),
        )

        val total = CalculadoraNotificacoes.contarDevidas(revisoes, assuntos, disciplinas, projetos, hoje)

        assertEquals(1, total)
    }

    @Test
    fun `sem projetos ativos nao ha devidas`() {
        val projetos = listOf(projeto(1, arquivado = true))
        val disciplinas = listOf(disciplina(1, 1))
        val assuntos = listOf(assunto(1, 1))
        val revisoes = listOf(revisao(1, 1, hoje))

        val total = CalculadoraNotificacoes.contarDevidas(revisoes, assuntos, disciplinas, projetos, hoje)

        assertEquals(0, total)
    }

    @Test
    fun `considera todos os projetos nao arquivados independentemente do selecionado`() {
        val projetos = listOf(projeto(1), projeto(2))
        val disciplinas = listOf(disciplina(1, 1), disciplina(2, 2))
        val assuntos = listOf(assunto(1, 1), assunto(2, 2))
        val revisoes = listOf(
            revisao(1, 1, hoje),
            revisao(2, 2, hoje.minusDays(2)),
        )

        val total = CalculadoraNotificacoes.contarDevidas(revisoes, assuntos, disciplinas, projetos, hoje)

        assertEquals(2, total)
    }
}
