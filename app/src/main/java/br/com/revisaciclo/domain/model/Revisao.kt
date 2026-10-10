package br.com.revisaciclo.domain.model

enum class StatusRevisao {
    PENDENTE,
    CONCLUIDA,
    ATRASADA,
}

data class Revisao(
    val id: Long,
    val assuntoId: Long,
    val agendadaPara: Long,
    val concluidaEm: Long?,
    val status: StatusRevisao,
    val intervaloDias: Int,
    val indiceCiclo: Int,
    val qualidade: Int?,
)
