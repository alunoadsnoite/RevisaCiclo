package br.com.revisaciclo.domain.model

/**
 * Projeto de estudo independente (concurso, certificação, faculdade, estudos pessoais).
 *
 * Cada disciplina pertence a exatamente um projeto. Projetos arquivados permanecem
 * com seus dados, mas deixam de ser oferecidos como projeto ativo.
 */
data class ProjetoEstudo(
    val id: Long,
    val nome: String,
    val descricao: String? = null,
    val cor: Long,
    val criadoEm: Long,
    val arquivado: Boolean = false,
)
