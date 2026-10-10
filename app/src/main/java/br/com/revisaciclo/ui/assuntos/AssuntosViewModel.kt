package br.com.revisaciclo.ui.assuntos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.repository.RevisaoRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssuntosUiState(
    val carregando: Boolean = true,
    val disciplina: Disciplina? = null,
    val assuntos: List<Assunto> = emptyList(),
)

class AssuntosViewModel(
    private val repository: RevisaoRepository,
    private val disciplinaId: Long,
) : ViewModel() {

    val uiState: StateFlow<AssuntosUiState> = combine(
        repository.observarDisciplina(disciplinaId),
        repository.observarAssuntosPorDisciplina(disciplinaId),
    ) { disciplina, assuntos ->
        AssuntosUiState(carregando = false, disciplina = disciplina, assuntos = assuntos)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssuntosUiState(),
    )

    fun salvar(id: Long?, titulo: String, descricao: String?, dataEstudo: LocalDate) {
        val tituloLimpo = titulo.trim()
        if (tituloLimpo.isEmpty()) return
        val descricaoLimpa = descricao?.trim()?.takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            if (id == null) {
                repository.criarAssunto(disciplinaId, tituloLimpo, descricaoLimpa, dataEstudo)
            } else {
                repository.atualizarAssunto(id, disciplinaId, tituloLimpo, descricaoLimpa, dataEstudo)
            }
        }
    }

    fun excluir(id: Long) {
        viewModelScope.launch { repository.excluirAssunto(id) }
    }

    companion object {
        fun factory(disciplinaId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                AssuntosViewModel(app.container.revisaoRepository, disciplinaId)
            }
        }
    }
}
