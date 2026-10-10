package br.com.revisaciclo.ui.disciplinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.repository.RevisaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DisciplinasUiState(
    val carregando: Boolean = true,
    val disciplinas: List<Disciplina> = emptyList(),
)

class DisciplinasViewModel(
    private val repository: RevisaoRepository,
) : ViewModel() {

    val uiState: StateFlow<DisciplinasUiState> = repository.observarDisciplinas()
        .map { disciplinas -> DisciplinasUiState(carregando = false, disciplinas = disciplinas) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DisciplinasUiState(),
        )

    fun salvar(id: Long?, nome: String, cor: Long) {
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isEmpty()) return
        viewModelScope.launch {
            if (id == null) {
                repository.criarDisciplina(nomeLimpo, cor)
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
                DisciplinasViewModel(app.container.revisaoRepository)
            }
        }
    }
}
