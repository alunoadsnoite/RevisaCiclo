package br.com.revisaciclo.domain.model

/**
 * Progresso da meta diária de revisões, calculado a partir de revisões concluídas
 * no dia corrente.
 */
data class ProgressoMeta(
    val concluidasHoje: Int,
    val metaDiaria: Int,
) {
    val atingida: Boolean
        get() = metaDiaria in 1..concluidasHoje

    val restantes: Int
        get() = (metaDiaria - concluidasHoje).coerceAtLeast(0)

    val percentual: Float
        get() = if (metaDiaria <= 0) 0f else (concluidasHoje.toFloat() / metaDiaria).coerceIn(0f, 1f)
}
