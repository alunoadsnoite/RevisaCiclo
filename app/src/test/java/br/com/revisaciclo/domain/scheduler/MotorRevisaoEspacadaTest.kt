package br.com.revisaciclo.domain.scheduler

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorRevisaoEspacadaTest {

    private val referencia = LocalDate.of(2026, 1, 1)

    @Test
    fun `agendamento inicial usa o primeiro intervalo`() {
        val agendamento = MotorRevisaoEspacada.agendamentoInicial(referencia)

        assertEquals(1, agendamento.ciclo)
        assertEquals(1, agendamento.intervaloDias)
        assertEquals(LocalDate.of(2026, 1, 2), agendamento.proximaRevisao)
    }

    @Test
    fun `intervalos crescem conforme o ciclo`() {
        assertEquals(1, MotorRevisaoEspacada.intervaloDias(1))
        assertEquals(3, MotorRevisaoEspacada.intervaloDias(2))
        assertEquals(7, MotorRevisaoEspacada.intervaloDias(3))
        assertEquals(15, MotorRevisaoEspacada.intervaloDias(4))
        assertEquals(30, MotorRevisaoEspacada.intervaloDias(5))
    }

    @Test
    fun `intervalo permanece em 30 dias apos o ultimo ciclo`() {
        assertEquals(30, MotorRevisaoEspacada.intervaloDias(6))
        assertEquals(30, MotorRevisaoEspacada.intervaloDias(20))
    }

    @Test
    fun `ciclo invalido cai no primeiro intervalo`() {
        assertEquals(1, MotorRevisaoEspacada.intervaloDias(0))
        assertEquals(1, MotorRevisaoEspacada.intervaloDias(-3))
    }

    @Test
    fun `esquecer reinicia o ciclo`() {
        assertEquals(1, MotorRevisaoEspacada.proximoCiclo(4, AvaliacaoRevisao.ESQUECI))
    }

    @Test
    fun `dificil repete o ciclo atual`() {
        assertEquals(3, MotorRevisaoEspacada.proximoCiclo(3, AvaliacaoRevisao.DIFICIL))
    }

    @Test
    fun `bom avanca um ciclo`() {
        assertEquals(3, MotorRevisaoEspacada.proximoCiclo(2, AvaliacaoRevisao.BOM))
    }

    @Test
    fun `facil avanca dois ciclos`() {
        assertEquals(4, MotorRevisaoEspacada.proximoCiclo(2, AvaliacaoRevisao.FACIL))
    }

    @Test
    fun `proximo agendamento parte da data de referencia`() {
        val agendamento = MotorRevisaoEspacada.proximoAgendamento(
            dataReferencia = referencia,
            cicloAtual = 2,
            avaliacao = AvaliacaoRevisao.BOM,
        )

        assertEquals(3, agendamento.ciclo)
        assertEquals(7, agendamento.intervaloDias)
        assertEquals(LocalDate.of(2026, 1, 8), agendamento.proximaRevisao)
    }

    @Test
    fun `esta atrasada quando a proxima revisao e anterior a hoje`() {
        assertTrue(MotorRevisaoEspacada.estaAtrasada(LocalDate.of(2025, 12, 31), referencia))
        assertFalse(MotorRevisaoEspacada.estaAtrasada(LocalDate.of(2026, 1, 1), referencia))
        assertFalse(MotorRevisaoEspacada.estaAtrasada(null, referencia))
    }
}
