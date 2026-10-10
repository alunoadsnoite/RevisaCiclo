package br.com.revisaciclo.domain.notificacao

import br.com.revisaciclo.domain.model.Preferencias
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisorNotificacaoTest {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val preferencias = Preferencias(
        notificacoesAtivas = true,
        horarioNotificacao = LocalTime.of(8, 0),
    )

    private fun agora(hora: Int, minuto: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 5, 20, hora, minuto, 0, 0, zona)

    @Test
    fun `nao notifica quando desativado`() {
        val desativado = preferencias.copy(notificacoesAtivas = false)
        assertFalse(
            DecisorNotificacao.deveNotificar(desativado, revisoesDevidas = 5, jaNotificadoNoDia = false, agora = agora(9, 0)),
        )
    }

    @Test
    fun `nao notifica sem revisoes devidas`() {
        assertFalse(
            DecisorNotificacao.deveNotificar(preferencias, revisoesDevidas = 0, jaNotificadoNoDia = false, agora = agora(9, 0)),
        )
    }

    @Test
    fun `nao notifica duas vezes no mesmo dia`() {
        assertFalse(
            DecisorNotificacao.deveNotificar(preferencias, revisoesDevidas = 3, jaNotificadoNoDia = true, agora = agora(10, 0)),
        )
    }

    @Test
    fun `nao notifica antes do horario preferido`() {
        assertFalse(
            DecisorNotificacao.deveNotificar(preferencias, revisoesDevidas = 3, jaNotificadoNoDia = false, agora = agora(7, 59)),
        )
    }

    @Test
    fun `notifica exatamente no horario preferido`() {
        assertTrue(
            DecisorNotificacao.deveNotificar(preferencias, revisoesDevidas = 3, jaNotificadoNoDia = false, agora = agora(8, 0)),
        )
    }

    @Test
    fun `notifica apos o horario preferido`() {
        assertTrue(
            DecisorNotificacao.deveNotificar(preferencias, revisoesDevidas = 1, jaNotificadoNoDia = false, agora = agora(21, 30)),
        )
    }
}
