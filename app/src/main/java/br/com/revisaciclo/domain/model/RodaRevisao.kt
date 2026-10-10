package br.com.revisaciclo.domain.model

data class RodaRevisao(
    val estagios: List<EstagioRoda>,
    val totalAssuntos: Int,
) {
    val estaVazia: Boolean
        get() = totalAssuntos == 0
}