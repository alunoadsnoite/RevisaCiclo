package br.com.revisaciclo.domain.sessao

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessaoDeRevisaoTest {

    private fun pendente(
        revisaoId: Long,
        atrasada: Boolean = false,
        agendadaPara: LocalDate = LocalDate.of(2026, 1, 10),
    ) = RevisaoPendente(
        revisaoId = revisaoId,
        assuntoId = 1L,
        assuntoTitulo = "Assunto $revisaoId",
        assuntoDescricao = null,
        disciplinaId = 1L,
        disciplinaNome = "Matemática",
        disciplinaCor = 0xFF1565C0,
        agendadaPara = agendadaPara,
        indiceCiclo = 1,
        intervaloDias = 1,
        atrasada = atrasada,
    )

    @Test
    fun `fila vazia nao possui item atual`() {
        val sessao = SessaoDeRevisao(emptyList())

        assertEquals(0, sessao.total)
        assertTrue(sessao.concluida)
        assertNull(sessao.atual)
        assertFalse(sessao.avancar(AvaliacaoRevisao.BOM))
    }

    @Test
    fun `avanca na ordem da fila acumulando contagens`() {
        val sessao = SessaoDeRevisao(
            listOf(pendente(1), pendente(2), pendente(3)),
        )

        assertEquals(3, sessao.total)
        assertEquals(1L, sessao.atual?.revisaoId)

        sessao.avancar(AvaliacaoRevisao.BOM)
        assertEquals(2L, sessao.atual?.revisaoId)
        assertEquals(1, sessao.concluidas)

        sessao.avancar(AvaliacaoRevisao.ESQUECI)
        assertEquals(3L, sessao.atual?.revisaoId)
        sessao.avancar(AvaliacaoRevisao.FACIL)

        assertTrue(sessao.concluida)
        assertEquals(3, sessao.concluidas)
        assertEquals(1, sessao.contagemResultados.getValue(AvaliacaoRevisao.BOM))
        assertEquals(1, sessao.contagemResultados.getValue(AvaliacaoRevisao.ESQUECI))
        assertEquals(1, sessao.contagemResultados.getValue(AvaliacaoRevisao.FACIL))
    }

    @Test
    fun `contagens iniciam zeradas para todas as avaliacoes`() {
        val sessao = SessaoDeRevisao(listOf(pendente(1)))

        AvaliacaoRevisao.entries.forEach { avaliacao ->
            assertEquals(0, sessao.contagemResultados.getValue(avaliacao))
        }
    }

    @Test
    fun `avancar apos o fim nao altera contagens`() {
        val sessao = SessaoDeRevisao(listOf(pendente(1)))
        sessao.avancar(AvaliacaoRevisao.BOM)

        assertFalse(sessao.avancar(AvaliacaoRevisao.FACIL))
        assertEquals(1, sessao.contagemResultados.getValue(AvaliacaoRevisao.BOM))
        assertEquals(0, sessao.contagemResultados.getValue(AvaliacaoRevisao.FACIL))
    }

    @Test
    fun `devidas incluem apenas revisoes agendadas ate hoje`() {
        val hoje = LocalDate.of(2026, 1, 10)
        val pendentes = listOf(
            pendente(1, agendadaPara = hoje.minusDays(1)),
            pendente(2, agendadaPara = hoje),
            pendente(3, agendadaPara = hoje.plusDays(1)),
        )

        val devidas = revisoesDevidas(pendentes, hoje)

        assertEquals(listOf(1L, 2L), devidas.map { it.revisaoId })
    }

    @Test
    fun `revisao agendada para amanha entra na fila na virada do dia`() {
        val amanha = LocalDate.of(2026, 1, 11)
        val pendente = pendente(1, agendadaPara = amanha, atrasada = false)

        assertTrue(revisoesDevidas(listOf(pendente), amanha.minusDays(1)).isEmpty())
        assertEquals(listOf(pendente), revisoesDevidas(listOf(pendente), amanha))
    }

    @Test
    fun `mesma data produz o mesmo conjunto de devidas independente do flag atrasada`() {
        val hoje = LocalDate.of(2026, 1, 10)
        val pendente = pendente(1, agendadaPara = hoje.minusDays(2), atrasada = false)

        assertEquals(listOf(pendente), revisoesDevidas(listOf(pendente), hoje))
    }

    @Test
    fun `lista vazia nao gera devidas`() {
        assertTrue(revisoesDevidas(emptyList(), LocalDate.of(2026, 1, 10)).isEmpty())
    }
}