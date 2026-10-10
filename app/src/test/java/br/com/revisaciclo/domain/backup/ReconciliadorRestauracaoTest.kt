package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.notificacao.AgendadorNotificacoes
import br.com.revisaciclo.domain.notificacao.GerenciadorNotificacoes
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconciliadorRestauracaoTest {

    private class EstadoFake(
        var pendente: EstadoOperacao? = EstadoOperacao(
            Preferencias(horarioNotificacao = LocalTime.of(9, 30), metaDiaria = 7),
        ),
    ) : RepositorioEstadoOperacao {
        var lidos = 0
        var limpos = 0
        override suspend fun ler(): EstadoOperacao? { lidos++; return pendente }
        override suspend fun gravar(estado: EstadoOperacao) {}
        override suspend fun limpar() { limpos++; pendente = null }
    }

    private class ArmazenamentoFake : ArmazenamentoBackup {
        var prefsAplicadas: Preferencias? = null
        override suspend fun capturar(): BackupDados {
            throw UnsupportedOperationException()
        }
        override suspend fun substituirRoom(dados: BackupDados) {}
        override suspend fun aplicarPreferencias(preferencias: Preferencias) { prefsAplicadas = preferencias }
        override suspend fun lerPreferencias(): Preferencias = Preferencias()
    }

    private class AgendadorFake : AgendadorNotificacoes {
        var reagendamentos = 0
        var ultima: Preferencias? = null
        override fun garantirAgendado(preferencias: Preferencias) {}
        override fun reagendar(preferencias: Preferencias) { reagendamentos++; ultima = preferencias }
        override fun cancelar() {}
    }

    @Test
    fun `reconcilia quando ha estado pendente`() = runBlocking {
        val estado = EstadoFake()
        val armazenamento = ArmazenamentoFake()
        val agendador = AgendadorFake()
        val reconciliador = ReconciliadorRestauracao(estado, armazenamento, GerenciadorNotificacoes(agendador))

        val ok = reconciliador.reconciliar()
        assertTrue(ok)
        assertEquals(9, armazenamento.prefsAplicadas?.horarioNotificacao?.hour)
        assertEquals(7, agendador.ultima?.metaDiaria)
        assertEquals(1, estado.limpos)
    }

    @Test
    fun `nao faz nada quando sem estado pendente`() = runBlocking {
        val estado = EstadoFake(pendente = null)
        val armazenamento = ArmazenamentoFake()
        val agendador = AgendadorFake()
        val reconciliador = ReconciliadorRestauracao(estado, armazenamento, GerenciadorNotificacoes(agendador))

        val ok = reconciliador.reconciliar()
        assertEquals(false, ok)
        assertEquals(null, armazenamento.prefsAplicadas)
        assertEquals(0, agendador.reagendamentos)
        assertEquals(0, estado.limpos)
    }
}
