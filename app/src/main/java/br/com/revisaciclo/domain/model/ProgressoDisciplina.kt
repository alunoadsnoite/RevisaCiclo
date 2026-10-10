package br.com.revisaciclo.domain.model

data class ProgressoDisciplina(
    val disciplinaId: Long,
    val nome: String,
    val cor: Long,
    val totalAssuntos: Int,
    val assuntosAtrasados: Int,
) {
    val progresso: Float
        get() = if (totalAssuntos == 0) {
            0f
        } else {
            (totalAssuntos - assuntosAtrasados).toFloat() / totalAssuntos
        }
}
