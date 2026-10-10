package br.com.revisaciclo.ui.revisoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.agregacao.FiltroProjeto
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.domain.projetos.observarProjetoAtivo
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.ProjetoRepository
import br.com.revisaciclo.domain.repository.RevisaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val preferenciasRepository: PreferenciasRepository,
    private val projetoRepository: ProjetoRepository,
) : ViewModel() {

    val uiState: StateFlow<RevisoesUiState> = combine(
        observarProjetoAtivo(projetoRepository, preferenciasRepository),
        repository.observarDisciplinas(),
        repository.observarRevisoesPendentes(),
    ) { projeto, disciplinas, pendentes ->
        val disciplinaIds = FiltroProjeto.disciplinas(disciplinas, projeto?.id)
            .map { it.id }
            .toSet()
        RevisoesUiState(
            carregando = false,
            pendentes = FiltroProjeto.revisoesPendentes(pendentes, disciplinaIds),
        )
    }.stateIn(
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
                RevisoesViewModel(
                    repository = app.container.revisaoRepository,
                    preferenciasRepository = app.container.preferenciasRepository,
                    projetoRepository = app.container.projetoRepository,
                )
            }
        }
    }
}
