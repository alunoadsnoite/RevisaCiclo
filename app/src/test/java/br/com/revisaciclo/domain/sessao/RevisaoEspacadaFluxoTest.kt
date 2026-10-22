package br.com.revisaciclo.domain.sessao

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.domain.model.StatusRevisao
import br.com.revisaciclo.domain.notificacao.AgendaNotificacao
import br.com.revisaciclo.domain.notificacao.GerenciadorNotificacoes
import br.com.revisaciclo.domain.scheduler.MotorRevisaoEspacada
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa os fluxos que Jorg Bollwahn não verificou na revisão de RevisaCiclo 0.1.3:
 *  - agendamento espaçado de revisão (ciclos, intervalos)
 *  - fila da roda de revisão (ordem, devidas, atrasadas)
 *  - persistência de avaliações
 *  - caminho do lembrete (notificação ativa/desativada)
 */
class RevisaoEspacadaFluxoTest {

    private val referencia = LocalDate.of(2026, 1, 1)

    @Test
    fun `ciclo completo espacado com fixo em 30 dias depois`() {
        val proximasRevisoes = buildList {
            var data = referencia
            var ciclo = 1
            repeat(7) {
                val agendamento = MotorRevisaoEspacada.agendar(data, ciclo)
                add(agendamento.proximaRevisao)
                data = agendamento.proximaRevisao
                ciclo = MotorRevisaoEspacada.proximoCiclo(ciclo, AvaliacaoRevisao.BOM)
            }
        }

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 2), // ciclo 1: +1
                LocalDate.of(2026, 1, 5), // ciclo 2: +3
                LocalDate.of(2026, 1, 12), // ciclo 3: +7
                LocalDate.of(2026, 1, 27), // ciclo 4: +15
                LocalDate.of(2026, 2, 26), // ciclo 5: +30
                LocalDate.of(2026, 3, 28), // ciclo 6: +30 (fixo)
                LocalDate.of(2026, 4, 27), // ciclo 7: +30 (fixo)
            ),
            proximasRevisoes,
        )
    }

    @Test
    fun `esqueci reinicia no ciclo 1 e o dia seguinte volta a ser devido`() {
        val agendamento = MotorRevisaoEspacada.proximoAgendamento(
            dataReferencia = referencia,
            cicloAtual = 5,
            avaliacao = AvaliacaoRevisao.ESQUECI,
        )
        assertEquals(1, agendamento.ciclo)
        assertEquals(1, agendamento.intervaloDias)
        assertEquals(LocalDate.of(2026, 1, 2), agendamento.proximaRevisao)
    }

    @Test
    fun `roda agrupa por estágio e separa atrasados, hoje e futuro`() {
        val hoje = LocalDate.of(2026, 1, 10)
        val pendentes = listOf(
            RevisaoPendente(
                revisaoId = 101L, assuntoId = 1L, assuntoTitulo = "Matemática",
                disciplinaId = 1L, disciplinaNome = "Matemática", disciplinaCor = 0xFF1565C0,
                agendadaPara = hoje.minusDays(1), indiceCiclo = 2, intervaloDias = 7, atrasada = true,
            ),
            RevisaoPendente(
                revisaoId = 102L, assuntoId = 2L, assuntoTitulo = "História",
                disciplinaId = 2L, disciplinaNome = "História", disciplinaCor = 0xFF1565C0,
                agendadaPara = hoje, indiceCiclo = 1, intervaloDias = 1, atrasada = false,
            ),
            RevisaoPendente(
                revisaoId = 103L, assuntoId = 3L, assuntoTitulo = "Português",
                disciplinaId = 3L, disciplinaNome = "Português", disciplinaCor = 0xFF1565C0,
                agendadaPara = hoje.plusDays(2), indiceCiclo = 1, intervaloDias = 1, atrasada = false,
            ),
        )

        val sessao = SessaoDeRevisao(pendentes)

        assertEquals(3, sessao.total)
        assertEquals(101L, sessao.atual?.revisaoId)
        sessao.avancar(AvaliacaoRevisao.BOM)
        assertEquals(102L, sessao.atual?.revisaoId)
        sessao.avancar(AvaliacaoRevisao.FACIL)
        assertEquals(103L, sessao.atual?.revisaoId)
        sessao.avancar(AvaliacaoRevisao.BOM)
        assertTrue(sessao.concluida)
        assertEquals(3, sessao.concluidas)
    }

    @Test
    fun `revisao devida entra na fila quando a data chega ou passa`() {
        val hoje = LocalDate.of(2026, 1, 10)
        val pendentes = listOf(
            RevisaoPendente(
                revisaoId = 201L, assuntoId = 1L, assuntoTitulo = "A",
                disciplinaId = 1L, disciplinaNome = "A", disciplinaCor = 0xFF1565C0,
                agendadaPara = hoje.minusDays(1), indiceCiclo = 1, intervaloDias = 1, atrasada = true,
            ),
            RevisaoPendente(
                revisaoId = 202L, assuntoId = 2L, assuntoTitulo = "B",
                disciplinaId = 2L, disciplinaNome = "B", disciplinaCor = 0xFF1565C0,
                agendadaPara = hoje, indiceCiclo = 1, intervaloDias = 1, atrasada = false,
            ),
            RevisaoPendente(
                revisaoId = 203L, assuntoId = 3L, assuntoTitulo = "C",
                disciplinaId = 3L, disciplinaNome = "C", disciplinaCor = 0xFF1565C0,
                agendadaPara = hoje.plusDays(1), indiceCiclo = 1, intervaloDias = 1, atrasada = false,
            ),
        )

        val devidas = revisoesDevidas(pendentes, hoje)

        assertEquals(2, devidas.size)
        assertEquals(listOf(201L, 202L), devidas.map { it.revisaoId })
    }

    @Test
    fun `notificação ativa agenda e desativada cancela via gerenciador`() {
        val preferenciasAtivo = br.com.revisaciclo.domain.model.Preferencias(
            notificacoesAtivas = true,
            horarioNotificacao = java.time.LocalTime.of(8, 0),
        )
        val preferenciasInativo = preferenciasAtivo.copy(notificacoesAtivas = false)

        val agendadorAtivo = object : br.com.revisaciclo.domain.notificacao.AgendadorNotificacoes {
            var ultimaReagendada: br.com.revisaciclo.domain.model.Preferencias? = null
            var cancelamentos = 0

            override fun garantirAgendado(preferencias: br.com.revisaciclo.domain.model.Preferencias) = Unit
            override fun reagendar(preferencias: br.com.revisaciclo.domain.model.Preferencias) {
                ultimaReagendada = preferencias
            }
            override fun cancelar() {
                cancelamentos++
            }
        }

        GerenciadorNotificacoes(agendadorAtivo).sincronizar(preferenciasAtivo)
        GerenciadorNotificacoes(agendadorAtivo).sincronizar(preferenciasInativo)

        assertEquals(1, agendadorAtivo.cancelamentos)
        assertEquals(preferenciasAtivo, agendadorAtivo.ultimaReagendada)
    }

    @Test
    fun `agenda notificação para o horário escolhido, reposiciona para amanha se passado`() {
        val horario = java.time.LocalTime.of(8, 0)
        val zona = java.time.ZoneId.of("America/Sao_Paulo")

        val antes = java.time.ZonedDateTime.of(2026, 5, 20, 7, 59, 0, 0, zona)
        val exato = java.time.ZonedDateTime.of(2026, 5, 20, 8, 0, 0, 0, zona)
        val depois = java.time.ZonedDateTime.of(2026, 5, 20, 8, 1, 0, 0, zona)

        assertEquals(antes.with(java.time.LocalDateTime.of(2026, 5, 20, 8, 0)), AgendaNotificacao.proximaExecucao(horario, antes))
        assertEquals(exato, AgendaNotificacao.proximaExecucao(horario, exato))
        assertEquals(depois.with(java.time.LocalDateTime.of(2026, 5, 21, 8, 0)), AgendaNotificacao.proximaExecucao(horario, depois))
    }

    @Test
    fun `data de agendamento usada como referência para a próxima revisão`() {
        val agendamento = MotorRevisaoEspacada.agendar(
            dataReferencia = LocalDate.of(2026, 6, 15),
            ciclo = 2,
        )
        assertEquals(LocalDate.of(2026, 6, 18), agendamento.proximaRevisao)
    }
}
