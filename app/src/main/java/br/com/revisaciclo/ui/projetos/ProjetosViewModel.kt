package br.com.revisaciclo.ui.projetos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.projetos.SeletorProjeto
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.ProjetoRepository
import br.com.revisaciclo.domain.repository.RevisaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjetosUiState(
    val carregando: Boolean = true,
    val projetos: List<ProjetoEstudo> = emptyList(),
    val projetoAtivo: ProjetoEstudo? = null,
    val contagensDisciplinas: Map<Long, Int> = emptyMap(),
) {
    val ativos: List<ProjetoEstudo>
        get() = projetos.filter { !it.arquivado }

    val arquivados: List<ProjetoEstudo>
        get() = projetos.filter { it.arquivado }

    fun contagem(projetoId: Long): Int = contagensDisciplinas[projetoId] ?: 0
}

class ProjetosViewModel(
    private val projetoRepository: ProjetoRepository,
    private val preferenciasRepository: PreferenciasRepository,
    revisaoRepository: RevisaoRepository,
) : ViewModel() {

    val uiState: StateFlow<ProjetosUiState> = combine(
        projetoRepository.observarProjetos(),
        preferenciasRepository.preferencias,
        revisaoRepository.observarDisciplinas(),
    ) { projetos, preferencias, disciplinas ->
        ProjetosUiState(
            carregando = false,
            projetos = projetos,
            projetoAtivo = SeletorProjeto.resolver(projetos, preferencias.projetoAtivoId),
            contagensDisciplinas = disciplinas.groupingBy { it.projetoId }.eachCount(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProjetosUiState(),
    )

    fun selecionar(id: Long) {
        viewModelScope.launch {
            preferenciasRepository.atualizar { it.copy(projetoAtivoId = id) }
        }
    }

    fun salvar(id: Long?, nome: String, descricao: String?, cor: Long) {
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isEmpty()) return
        val descricaoLimpa = descricao?.trim()?.takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            if (id == null) {
                projetoRepository.criar(nomeLimpo, descricaoLimpa, cor)
            } else {
                projetoRepository.atualizar(id, nomeLimpo, descricaoLimpa, cor)
            }
        }
    }

    fun arquivar(id: Long) {
        viewModelScope.launch { projetoRepository.definirArquivado(id, true) }
    }

    fun desarquivar(id: Long) {
        viewModelScope.launch { projetoRepository.definirArquivado(id, false) }
    }

    fun excluir(id: Long) {
        viewModelScope.launch { projetoRepository.excluir(id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                ProjetosViewModel(
                    projetoRepository = app.container.projetoRepository,
                    preferenciasRepository = app.container.preferenciasRepository,
                    revisaoRepository = app.container.revisaoRepository,
                )
            }
        }
    }
}
