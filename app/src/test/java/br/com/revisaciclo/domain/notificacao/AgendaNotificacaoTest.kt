package br.com.revisaciclo.domain.notificacao

import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AgendaNotificacaoTest {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val horario = LocalTime.of(8, 0)

    private fun agora(hora: Int, minuto: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 5, 20, hora, minuto, 0, 0, zona)

    @Test
    fun `antes do horario agenda para hoje`() {
        val proxima = AgendaNotificacao.proximaExecucao(horario, agora(7, 0))

        assertEquals(agora(8, 0), proxima)
    }

    @Test
    fun `exatamente no horario agenda para hoje`() {
        val proxima = AgendaNotificacao.proximaExecucao(horario, agora(8, 0))

        assertEquals(agora(8, 0), proxima)
        assertEquals(Duration.ZERO, AgendaNotificacao.atraso(horario, agora(8, 0)))
    }

    @Test
    fun `apos o horario agenda para amanha`() {
        val proxima = AgendaNotificacao.proximaExecucao(horario, agora(8, 30))

        assertEquals(agora(8, 0).plusDays(1), proxima)
    }

    @Test
    fun `atraso reflete a diferenca ate o proximo horario`() {
        assertEquals(Duration.ofMinutes(30), AgendaNotificacao.atraso(horario, agora(7, 30)))
    }

    @Test
    fun `atraso nunca e negativo`() {
        val atraso = AgendaNotificacao.atraso(horario, agora(9, 0))

        assertEquals(Duration.ofHours(23), atraso)
    }
}
