package br.com.revisaciclo.domain.model

data class ProgressoDisciplina(
    val disciplinaId: Long,
    val nome: String,
    val cor: Long,
    val totalRevisoes: Int,
    val concluidas: Int,
) {
    val progresso: Float
        get() = if (totalRevisoes == 0) 0f else concluidas.toFloat() / totalRevisoes
}
