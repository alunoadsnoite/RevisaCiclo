package br.com.revisaciclo.domain.model

import java.time.LocalDate

data class Assunto(
    val id: Long,
    val disciplinaId: Long,
    val titulo: String,
    val descricao: String?,
    val dataEstudo: LocalDate,
    val estadoRevisao: EstadoRevisao,
    val indiceCiclo: Int,
    val proximaRevisao: LocalDate?,
)
