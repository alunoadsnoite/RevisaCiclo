package br.com.revisaciclo.ui.disciplinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.agregacao.FiltroProjeto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.projetos.observarProjetoAtivo
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.ProjetoRepository
import br.com.revisaciclo.domain.repository.RevisaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DisciplinasUiState(
    val carregando: Boolean = true,
    val projetoAtivo: ProjetoEstudo? = null,
    val disciplinas: List<Disciplina> = emptyList(),
)

class DisciplinasViewModel(
    private val repository: RevisaoRepository,
    private val preferenciasRepository: PreferenciasRepository,
    private val projetoRepository: ProjetoRepository,
) : ViewModel() {

    val uiState: StateFlow<DisciplinasUiState> = combine(
        observarProjetoAtivo(projetoRepository, preferenciasRepository),
        repository.observarDisciplinas(),
    ) { projeto, disciplinas ->
        DisciplinasUiState(
            carregando = false,
            projetoAtivo = projeto,
            disciplinas = FiltroProjeto.disciplinas(disciplinas, projeto?.id),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DisciplinasUiState(),
    )

    fun salvar(id: Long?, nome: String, cor: Long) {
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isEmpty()) return
        viewModelScope.launch {
            if (id == null) {
                val projetoId = uiState.value.projetoAtivo?.id ?: return@launch
                repository.criarDisciplina(projetoId, nomeLimpo, cor)
            } else {
                repository.atualizarDisciplina(id, nomeLimpo, cor)
            }
        }
    }

    fun excluir(id: Long) {
        viewModelScope.launch { repository.excluirDisciplina(id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                DisciplinasViewModel(
                    repository = app.container.revisaoRepository,
                    preferenciasRepository = app.container.preferenciasRepository,
                    projetoRepository = app.container.projetoRepository,
                )
            }
        }
    }
}
