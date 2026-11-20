package br.com.revisaciclo.ui.revisoes

import br.com.revisaciclo.domain.model.RevisaoPendente
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RevisoesUiStateTest {
    private val hoje = LocalDate.of(2026, 11, 20)

    @Test
    fun filtroIncluiAtrasadasMaisAntigasPrimeiroEExcluiFuturas() {
        val estado = RevisoesUiState(
            hoje = hoje,
            carregando = false,
            revisoes = listOf(revisao(1), revisao(0), revisao(-1), revisao(-3)),
        )

        assertEquals(listOf(-3L, -1L, 0L, 1L), estado.pendentes.map { it.revisaoId })
        assertEquals(listOf(-3L, -1L, 0L), estado.paraHoje.map { it.revisaoId })
        assertEquals(listOf(true, true, false), estado.paraHoje.map { it.atrasada })
        assertEquals(2, estado.atrasadas)
    }

    @Test
    fun revisaoFuturaEntraNoFiltroNaDataAgendada() {
        val revisoes = listOf(revisao(1))
        assertEquals(emptyList<RevisaoPendente>(), RevisoesUiState(hoje, revisoes = revisoes).paraHoje)
        assertEquals(listOf(1L), RevisoesUiState(hoje.plusDays(1), revisoes = revisoes).paraHoje.map { it.revisaoId })
    }

    private fun revisao(dias: Long) = RevisaoPendente(
        revisaoId = dias,
        assuntoId = 1,
        assuntoTitulo = "Assunto",
        disciplinaId = 1,
        disciplinaNome = "Disciplina",
        disciplinaCor = 0,
        agendadaPara = hoje.plusDays(dias),
        indiceCiclo = 1,
        intervaloDias = 3,
        atrasada = false,
    )
}
