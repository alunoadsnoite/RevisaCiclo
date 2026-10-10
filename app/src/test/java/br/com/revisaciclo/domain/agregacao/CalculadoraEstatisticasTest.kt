package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraEstatisticasTest {

    private val hoje = LocalDate.of(2026, 1, 10)

    private fun revisao(
        id: Long,
        status: StatusRevisao = StatusRevisao.CONCLUIDA,
        concluidaEm: LocalDate? = hoje.minusDays(1),
        agendadaPara: LocalDate = hoje.minusDays(2),
    ) = Revisao(
        id = id,
        assuntoId = 1L,
        agendadaPara = agendadaPara,
        concluidaEm = concluidaEm,
        status = status,
        intervaloDias = 1,
        indiceCiclo = 1,
        avaliacao = AvaliacaoRevisao.BOM,
    )

    @Test
    fun `resumo conta concluidas pendentes e atrasadas`() {
        val revisoes = listOf(
            revisao(1, StatusRevisao.CONCLUIDA, hoje.minusDays(1)),
            revisao(2, StatusRevisao.PENDENTE, null, hoje.plusDays(1)),
            revisao(3, StatusRevisao.ATRASADA, null, hoje.minusDays(3)),
        )

        val estatisticas = CalculadoraEstatisticas.calcular(revisoes, hoje)

        assertEquals(3, estatisticas.totalRevisoes)
        assertEquals(1, estatisticas.concluidas)
        assertEquals(2, estatisticas.pendentes)
        assertEquals(1, estatisticas.atrasadas)
    }

    @Test
    fun `evolucao diaria preenche os dias sem revisao com zero`() {
        val concluidaOntem = revisao(1, StatusRevisao.CONCLUIDA, hoje.minusDays(3))
        val estatisticas = CalculadoraEstatisticas.calcular(listOf(concluidaOntem), hoje)

        assertEquals(7, estatisticas.evolucao7Dias.size)
        assertEquals(hoje.minusDays(6), estatisticas.evolucao7Dias.first().data)
        assertEquals(hoje, estatisticas.evolucao7Dias.last().data)
        assertEquals(1, estatisticas.evolucao7Dias[3].quantidade)
        assertEquals(0, estatisticas.evolucao7Dias[2].quantidade)
        assertEquals(0, estatisticas.evolucao7Dias[6].quantidade)
    }

    @Test
    fun `evolucao de 30 dias mantem o mesmo tamanho da janela`() {
        val estatisticas = CalculadoraEstatisticas.calcular(emptyList(), hoje)

        assertEquals(30, estatisticas.evolucao30Dias.size)
        assertEquals(hoje.minusDays(29), estatisticas.evolucao30Dias.first().data)
        assertEquals(hoje, estatisticas.evolucao30Dias.last().data)
        estatisticas.evolucao30Dias.forEach { assertEquals(0, it.quantidade) }
    }

    @Test
    fun `evolucao ignora revisoes pendentes e conclusoes fora da janela`() {
        val revisoes = listOf(
            revisao(1, StatusRevisao.PENDENTE, null, hoje),
            revisao(2, StatusRevisao.CONCLUIDA, hoje.minusDays(8)),
        )

        val estatisticas = CalculadoraEstatisticas.calcular(revisoes, hoje)

        assertEquals(7, estatisticas.evolucao7Dias.size)
        estatisticas.evolucao7Dias.forEach { assertEquals(0, it.quantidade) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `janela invalida lanca excecao`() {
        CalculadoraEstatisticas.evolucaoDiaria(emptyList(), hoje, 0)
    }
}