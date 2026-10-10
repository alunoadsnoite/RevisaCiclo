package br.com.revisaciclo.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardUiStateTest {

    @Test
    fun `progresso geral e calculado corretamente`() {
        val estado = DashboardUiState(totalRevisoes = 4, revisoesConcluidas = 3)

        assertEquals(0.75f, estado.progressoGeral, 0.0001f)
    }

    @Test
    fun `progresso geral e zero quando nao ha revisoes`() {
        val estado = DashboardUiState(totalRevisoes = 0, revisoesConcluidas = 0)

        assertEquals(0f, estado.progressoGeral, 0.0001f)
    }

    @Test
    fun `revisoes pendentes nao ficam negativas`() {
        val estado = DashboardUiState(totalRevisoes = 2, revisoesConcluidas = 5)

        assertEquals(0, estado.revisoesPendentes)
    }
}
