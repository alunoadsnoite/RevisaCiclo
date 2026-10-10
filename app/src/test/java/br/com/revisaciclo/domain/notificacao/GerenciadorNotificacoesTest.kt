package br.com.revisaciclo.domain.notificacao

import br.com.revisaciclo.domain.model.Preferencias
import org.junit.Assert.assertEquals
import org.junit.Test

class GerenciadorNotificacoesTest {

    private class AgendadorFake : AgendadorNotificacoes {
        var reagendamentos = 0
        var cancelamentos = 0
        var ultimaAgendada: Preferencias? = null

        override fun garantirAgendado(preferencias: Preferencias) = Unit

        override fun reagendar(preferencias: Preferencias) {
            reagendamentos++
            ultimaAgendada = preferencias
        }

        override fun cancelar() {
            cancelamentos++
        }
    }

    @Test
    fun `notificacoes ativas reagendam`() {
        val agendador = AgendadorFake()

        GerenciadorNotificacoes(agendador).sincronizar(Preferencias(notificacoesAtivas = true))

        assertEquals(1, agendador.reagendamentos)
        assertEquals(0, agendador.cancelamentos)
    }

    @Test
    fun `notificacoes desativadas cancelam`() {
        val agendador = AgendadorFake()

        GerenciadorNotificacoes(agendador).sincronizar(Preferencias(notificacoesAtivas = false))

        assertEquals(0, agendador.reagendamentos)
        assertEquals(1, agendador.cancelamentos)
    }

    @Test
    fun `reagenda usando as preferencias informadas`() {
        val agendador = AgendadorFake()
        val preferencias = Preferencias(notificacoesAtivas = true, metaDiaria = 7)

        GerenciadorNotificacoes(agendador).sincronizar(preferencias)

        assertEquals(preferencias, agendador.ultimaAgendada)
    }
}
