package br.com.revisaciclo.ui.sessao

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.domain.sessao.SessaoDeRevisao
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodeAvaliarTest {

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
    fun `bloqueia durante o carregamento`() {
        val estado = SessaoRevisaoUiState(carregando = true)
        assertFalse(podeAvaliar(estado))
    }

    @Test
    fun `bloqueia duplo toque enquanto persiste`() {
        val estado = SessaoRevisaoUiState(
            carregando = false,
            sessao = SessaoDeRevisao(listOf(pendente(1))),
            processando = true,
        )
        assertFalse(podeAvaliar(estado))
    }

    @Test
    fun `bloqueia quando a sessao esta vazia`() {
        val estado = SessaoRevisaoUiState(
            carregando = false,
            sessao = SessaoDeRevisao(emptyList()),
        )
        assertFalse(podeAvaliar(estado))
    }

    @Test
    fun `bloqueia quando a sessao ja foi concluida`() {
        val sessao = SessaoDeRevisao(listOf(pendente(1)))
        sessao.avancar(AvaliacaoRevisao.BOM)
        val estado = SessaoRevisaoUiState(carregando = false, sessao = sessao)

        assertFalse(podeAvaliar(estado))
        assertTrue(estado.concluida)
    }

    @Test
    fun `permite avaliar com item atual e sem operacao em andamento`() {
        val estado = SessaoRevisaoUiState(
            carregando = false,
            sessao = SessaoDeRevisao(listOf(pendente(1))),
        )
        assertTrue(podeAvaliar(estado))
    }
}