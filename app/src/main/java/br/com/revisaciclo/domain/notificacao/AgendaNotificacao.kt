package br.com.revisaciclo.domain.notificacao

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Cálculo puro do próximo horário de verificação, usado para agendar o
 * WorkManager. Trabalhar com [ZonedDateTime] garante que mudanças de data, fuso e
 * horário de verão sejam respeitadas a cada reagendamento.
 */
object AgendaNotificacao {

    fun proximaExecucao(horario: LocalTime, agora: ZonedDateTime): ZonedDateTime {
        val alvoHoje = agora.toLocalDate().atTime(horario).atZone(agora.zone)
        return if (!alvoHoje.isBefore(agora)) alvoHoje else alvoHoje.plusDays(1)
    }

    fun atraso(horario: LocalTime, agora: ZonedDateTime): Duration =
        Duration.between(agora, proximaExecucao(horario, agora))
}
