package br.com.revisaciclo.domain.model

data class Assunto(
    val id: Long,
    val disciplinaId: Long,
    val nome: String,
    val dificuldade: Int,
    val criadoEm: Long,
)
