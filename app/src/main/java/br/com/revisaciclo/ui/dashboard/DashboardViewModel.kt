package br.com.revisaciclo.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import br.com.revisaciclo.domain.repository.RevisaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val carregando: Boolean = true,
    val usaDadosDeDemonstracao: Boolean = true,
    val totalDisciplinas: Int = 0,
    val totalAssuntos: Int = 0,
    val totalRevisoes: Int = 0,
    val revisoesConcluidas: Int = 0,
    val proximaRevisao: Revisao? = null,
    val progressos: List<ProgressoDisciplina> = emptyList(),
) {
    val progressoGeral: Float
        get() = if (totalRevisoes == 0) 0f else revisoesConcluidas.toFloat() / totalRevisoes

    val revisoesPendentes: Int
        get() = (totalRevisoes - revisoesConcluidas).coerceAtLeast(0)
}

class DashboardViewModel(
    private val repository: RevisaoRepository,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observarDisciplinas(),
        repository.observarAssuntos(),
        repository.observarRevisoes(),
        repository.observarProgressoPorDisciplina(),
    ) { disciplinas, assuntos, revisoes, progressos ->
        DashboardUiState(
            carregando = false,
            totalDisciplinas = disciplinas.size,
            totalAssuntos = assuntos.size,
            totalRevisoes = revisoes.size,
            revisoesConcluidas = revisoes.count { it.status == StatusRevisao.CONCLUIDA },
            proximaRevisao = revisoes
                .filter { it.status != StatusRevisao.CONCLUIDA }
                .minByOrNull { it.agendadaPara },
            progressos = progressos,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    init {
        viewModelScope.launch {
            repository.garantirDadosDeDemonstracao()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                DashboardViewModel(app.container.revisaoRepository)
            }
        }
    }
}
