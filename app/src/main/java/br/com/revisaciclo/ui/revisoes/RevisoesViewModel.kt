package br.com.revisaciclo.ui.revisoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.domain.repository.RevisaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RevisoesUiState(
    val carregando: Boolean = true,
    val pendentes: List<RevisaoPendente> = emptyList(),
) {
    val atrasadas: Int
        get() = pendentes.count { it.atrasada }
}

class RevisoesViewModel(
    private val repository: RevisaoRepository,
) : ViewModel() {

    val uiState: StateFlow<RevisoesUiState> = repository.observarRevisoesPendentes()
        .map { pendentes -> RevisoesUiState(carregando = false, pendentes = pendentes) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RevisoesUiState(),
        )

    fun avaliar(revisaoId: Long, avaliacao: AvaliacaoRevisao) {
        viewModelScope.launch { repository.concluirRevisao(revisaoId, avaliacao) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                RevisoesViewModel(app.container.revisaoRepository)
            }
        }
    }
}
