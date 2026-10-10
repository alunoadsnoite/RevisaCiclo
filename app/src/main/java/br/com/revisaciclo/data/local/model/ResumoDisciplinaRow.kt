package br.com.revisaciclo.data.local.model

data class ResumoDisciplinaRow(
    val disciplinaId: Long,
    val nome: String,
    val cor: Long,
    val totalRevisoes: Int,
    val concluidas: Int,
)
