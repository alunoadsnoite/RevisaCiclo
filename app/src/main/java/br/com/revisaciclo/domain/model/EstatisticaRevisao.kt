package br.com.revisaciclo.domain.model

import java.time.LocalDate

data class ContagemDiaria(
    val data: LocalDate,
    val quantidade: Int,
)

data class EstatisticaRevisao(
    val totalRevisoes: Int,
    val concluidas: Int,
    val pendentes: Int,
    val atrasadas: Int,
    val evolucao7Dias: List<ContagemDiaria>,
    val evolucao30Dias: List<ContagemDiaria>,
)