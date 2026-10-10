package br.com.revisaciclo.domain.model

import java.time.LocalDate

data class AssuntoRoda(
    val id: Long,
    val titulo: String,
    val disciplinaNome: String,
    val disciplinaCor: Long,
    val intervaloDias: Int,
    val proximaRevisao: LocalDate?,
    val situacao: SituacaoRevisao,
)

data class EstagioRoda(
    val intervaloDias: Int,
    val assuntos: List<AssuntoRoda>,
) {
    val total: Int
        get() = assuntos.size

    val atrasados: Int
        get() = assuntos.count { it.situacao == SituacaoRevisao.ATRASADO }

    val hoje: Int
        get() = assuntos.count { it.situacao == SituacaoRevisao.HOJE }
}