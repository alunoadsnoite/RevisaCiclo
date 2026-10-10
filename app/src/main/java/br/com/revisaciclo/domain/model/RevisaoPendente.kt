package br.com.revisaciclo.domain.model

import java.time.LocalDate

data class RevisaoPendente(
    val revisaoId: Long,
    val assuntoId: Long,
    val assuntoTitulo: String,
    val disciplinaId: Long,
    val disciplinaNome: String,
    val disciplinaCor: Long,
    val agendadaPara: LocalDate,
    val indiceCiclo: Int,
    val intervaloDias: Int,
    val atrasada: Boolean,
)
