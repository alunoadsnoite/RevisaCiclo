package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculadoraMetaTest {

    private val hoje = LocalDate.of(2026, 3, 10)

    private fun revisao(
        id: Long,
        status: StatusRevisao,
        concluidaEm: LocalDate? = null,
    ) = Revisao(
        id = id,
        assuntoId = id,
        agendadaPara = hoje,
        concluidaEm = concluidaEm,
        status = status,
        intervaloDias = 1,
        indiceCiclo = 1,
        avaliacao = if (status == StatusRevisao.CONCLUIDA) AvaliacaoRevisao.BOM else null,
    )

    @Test
    fun `sem revisoes o progresso e zero`() {
        val progresso = CalculadoraMeta.calcular(emptyList(), hoje, metaDiaria = 20)

        assertEquals(0, progresso.concluidasHoje)
        assertEquals(20, progresso.metaDiaria)
        assertFalse(progresso.atingida)
    }

    @Test
    fun `conta apenas revisoes concluidas hoje`() {
        val revisoes = listOf(
            revisao(1, StatusRevisao.CONCLUIDA, concluidaEm = hoje),
            revisao(2, StatusRevisao.CONCLUIDA, concluidaEm = hoje),
            revisao(3, StatusRevisao.CONCLUIDA, concluidaEm = hoje.minusDays(1)),
            revisao(4, StatusRevisao.PENDENTE),
        )

        val progresso = CalculadoraMeta.calcular(revisoes, hoje, metaDiaria = 5)

        assertEquals(2, progresso.concluidasHoje)
        assertEquals(3, progresso.restantes)
        assertFalse(progresso.atingida)
    }

    @Test
    fun `virada do dia zera a contagem`() {
        val concluidaOntem = revisao(1, StatusRevisao.CONCLUIDA, concluidaEm = hoje.minusDays(1))

        val progressoOntem = CalculadoraMeta.calcular(listOf(concluidaOntem), hoje.minusDays(1), metaDiaria = 3)
        val progressoHoje = CalculadoraMeta.calcular(listOf(concluidaOntem), hoje, metaDiaria = 3)

        assertEquals(1, progressoOntem.concluidasHoje)
        assertEquals(0, progressoHoje.concluidasHoje)
    }

    @Test
    fun `meta atingida quando concluidas alcancam o alvo`() {
        val revisoes = (1L..3L).map { revisao(it, StatusRevisao.CONCLUIDA, concluidaEm = hoje) }

        val progresso = CalculadoraMeta.calcular(revisoes, hoje, metaDiaria = 3)

        assertTrue(progresso.atingida)
        assertEquals(0, progresso.restantes)
        assertEquals(1f, progresso.percentual, 0.0001f)
    }

    @Test
    fun `percentual e limitado a um`() {
        val revisoes = (1L..5L).map { revisao(it, StatusRevisao.CONCLUIDA, concluidaEm = hoje) }

        val progresso = CalculadoraMeta.calcular(revisoes, hoje, metaDiaria = 2)

        assertEquals(1f, progresso.percentual, 0.0001f)
        assertEquals(0, progresso.restantes)
    }

    @Test
    fun `percentual proporcional quando parcial`() {
        val revisoes = (1L..2L).map { revisao(it, StatusRevisao.CONCLUIDA, concluidaEm = hoje) }

        val progresso = CalculadoraMeta.calcular(revisoes, hoje, metaDiaria = 4)

        assertEquals(0.5f, progresso.percentual, 0.0001f)
    }
}
