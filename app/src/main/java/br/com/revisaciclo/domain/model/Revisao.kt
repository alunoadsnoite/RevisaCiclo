package br.com.revisaciclo.domain.model

import java.time.LocalDate

enum class StatusRevisao {
    PENDENTE,
    CONCLUIDA,
    ATRASADA,
}

data class Revisao(
    val id: Long,
    val assuntoId: Long,
    val agendadaPara: LocalDate,
    val concluidaEm: LocalDate?,
    val status: StatusRevisao,
    val intervaloDias: Int,
    val indiceCiclo: Int,
    val avaliacao: AvaliacaoRevisao?,
)
