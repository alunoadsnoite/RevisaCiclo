package br.com.revisaciclo.domain.scheduler

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Garante que a parametrização de intervalos não quebrou o comportamento padrão
 * (regressão) e que intervalos personalizados são respeitados sem duplicar lógica.
 */
class MotorRevisaoEspacadaIntervalosTest {

    private val referencia = LocalDate.of(2026, 1, 1)
    private val personalizados = listOf(2, 5, 9)

    @Test
    fun `padrao permanece inalterado com um intervalo`() {
        val agendamento = MotorRevisaoEspacada.agendamentoInicial(referencia)

        assertEquals(1, agendamento.ciclo)
        assertEquals(1, agendamento.intervaloDias)
        assertEquals(LocalDate.of(2026, 1, 2), agendamento.proximaRevisao)
    }

    @Test
    fun `intervalos personalizados sao usados no agendamento inicial`() {
        val agendamento = MotorRevisaoEspacada.agendamentoInicial(referencia, personalizados)

        assertEquals(1, agendamento.ciclo)
        assertEquals(2, agendamento.intervaloDias)
        assertEquals(LocalDate.of(2026, 1, 3), agendamento.proximaRevisao)
    }

    @Test
    fun `intervaloDias respeita lista personalizada`() {
        assertEquals(2, MotorRevisaoEspacada.intervaloDias(1, personalizados))
        assertEquals(5, MotorRevisaoEspacada.intervaloDias(2, personalizados))
        assertEquals(9, MotorRevisaoEspacada.intervaloDias(3, personalizados))
    }

    @Test
    fun `apos o ultimo estagio usa o ultimo intervalo personalizado`() {
        assertEquals(9, MotorRevisaoEspacada.intervaloDias(4, personalizados))
        assertEquals(9, MotorRevisaoEspacada.intervaloDias(50, personalizados))
    }

    @Test
    fun `proximo agendamento usa intervalos personalizados`() {
        val agendamento = MotorRevisaoEspacada.proximoAgendamento(
            dataReferencia = referencia,
            cicloAtual = 1,
            avaliacao = AvaliacaoRevisao.BOM,
            intervalos = personalizados,
        )

        assertEquals(2, agendamento.ciclo)
        assertEquals(5, agendamento.intervaloDias)
        assertEquals(LocalDate.of(2026, 1, 6), agendamento.proximaRevisao)
    }

    @Test
    fun `esquecer reinicia no primeiro intervalo personalizado`() {
        val agendamento = MotorRevisaoEspacada.proximoAgendamento(
            dataReferencia = referencia,
            cicloAtual = 3,
            avaliacao = AvaliacaoRevisao.ESQUECI,
            intervalos = personalizados,
        )

        assertEquals(1, agendamento.ciclo)
        assertEquals(2, agendamento.intervaloDias)
    }
}
