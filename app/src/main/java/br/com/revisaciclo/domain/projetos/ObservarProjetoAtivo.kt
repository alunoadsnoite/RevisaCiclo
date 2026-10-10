package br.com.revisaciclo.domain.projetos

import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.ProjetoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Fluxo do projeto ativo, combinando a lista de projetos com a preferência persistida.
 *
 * Aplica [SeletorProjeto.resolver] para garantir fallback consistente quando não há
 * projeto selecionado, o selecionado foi arquivado ou deixou de existir.
 */
fun observarProjetoAtivo(
    projetoRepository: ProjetoRepository,
    preferenciasRepository: PreferenciasRepository,
): Flow<ProjetoEstudo?> = combine(
    projetoRepository.observarProjetos(),
    preferenciasRepository.preferencias,
) { projetos, preferencias ->
    SeletorProjeto.resolver(projetos, preferencias.projetoAtivoId)
}
