package br.com.revisaciclo.domain.sessao

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvaliarProximoTest {

    private fun pendente(revisaoId: Long) = RevisaoPendente(
        revisaoId = revisaoId,
        assuntoId = 1L,
        assuntoTitulo = "Assunto $revisaoId",
        assuntoDescricao = null,
        disciplinaId = 1L,
        disciplinaNome = "Matemática",
        disciplinaCor = 0xFF1565C0,
        agendadaPara = LocalDate.of(2026, 1, 10),
        indiceCiclo = 1,
        intervaloDias = 1,
        atrasada = false,
    )

    @Test
    fun `persiste a avaliacao antes de avancar a sessao`() = runBlocking {
        val efeito = mutableListOf<String>()
        val sessao = SessaoDeRevisao(listOf(pendente(1), pendente(2)))

        val avancou = avaliarProximo(
            sessao = sessao,
            persistir = { atual, avaliacao ->
                efeito += "persistir:${atual.revisaoId}:${avaliacao.name}"
            },
            avaliacao = AvaliacaoRevisao.BOM,
        )

        assertTrue(avancou)
        assertEquals(2L, sessao.atual?.revisaoId)
        assertEquals(listOf("persistir:1:BOM"), efeito)
    }

    @Test
    fun `falha na persistencia mantem o item na fila`() = runBlocking {
        val sessao = SessaoDeRevisao(listOf(pendente(1)))

        runCatching {
            avaliarProximo(
                sessao = sessao,
                persistir = { _, _ -> throw IllegalStateException("falha ao salvar") },
                avaliacao = AvaliacaoRevisao.FACIL,
            )
        }

        assertEquals(1L, sessao.atual?.revisaoId)
        assertEquals(0, sessao.concluidas)
        assertEquals(0, sessao.contagemResultados.getValue(AvaliacaoRevisao.FACIL))
    }

    @Test
    fun `sem item atual retorna falso e nada e persistido`() = runBlocking {
        val sessao = SessaoDeRevisao(emptyList())

        val avancou = avaliarProximo(
            sessao = sessao,
            persistir = { _, _ -> error("não deve persistir") },
            avaliacao = AvaliacaoRevisao.BOM,
        )

        assertFalse(avancou)
    }
}