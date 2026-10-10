package br.com.revisaciclo.domain.projetos

import br.com.revisaciclo.domain.model.ProjetoEstudo

/**
 * Define qual projeto está ativo a partir da lista de projetos e do id selecionado.
 *
 * - Se o id selecionado aponta para um projeto não arquivado, ele permanece ativo.
 * - Caso contrário (nulo, inexistente ou arquivado), usa o projeto ativo mais antigo.
 * - Sem projetos ativos, o resultado é nulo.
 */
object SeletorProjeto {

    fun resolver(projetos: List<ProjetoEstudo>, selecionadoId: Long?): ProjetoEstudo? {
        val ativos = projetos.filter { !it.arquivado }.sortedBy { it.criadoEm }
        if (ativos.isEmpty()) return null
        return ativos.firstOrNull { it.id == selecionadoId } ?: ativos.first()
    }
}
