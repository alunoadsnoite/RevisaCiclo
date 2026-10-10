package br.com.revisaciclo.domain.scheduler

import java.time.LocalDate

data class Agendamento(
    val ciclo: Int,
    val intervaloDias: Int,
    val proximaRevisao: LocalDate,
)
