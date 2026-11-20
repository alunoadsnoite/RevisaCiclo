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
import br.com.revisaciclo.domain.sessao.revisoesDevidas
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RevisoesUiState(
    hoje: LocalDate,
    val carregando: Boolean = true,
    revisoes: List<RevisaoPendente> = emptyList(),
) {
    val pendentes = revisoes.sortedBy { it.agendadaPara }
        .map { it.copy(atrasada = it.agendadaPara.isBefore(hoje)) }
    val paraHoje = revisoesDevidas(pendentes, hoje)
    val atrasadas = paraHoje.count { it.atrasada }
}

class RevisoesViewModel(
    private val repository: RevisaoRepository,
    private val preferenciasRepository: PreferenciasRepository,
    private val projetoRepository: ProjetoRepository,
    private val clock: Clock,
) : ViewModel() {
    private val hoje = MutableStateFlow(LocalDate.now(clock))

    val uiState: StateFlow<RevisoesUiState> = combine(
        observarProjetoAtivo(projetoRepository, preferenciasRepository),
        repository.observarDisciplinas(),
        repository.observarRevisoesPendentes(),
        hoje,
    ) { projeto, disciplinas, pendentes, data ->
        val disciplinaIds = FiltroProjeto.disciplinas(disciplinas, projeto?.id)
            .map { it.id }
            .toSet()
        RevisoesUiState(
            hoje = data,
            carregando = false,
            revisoes = FiltroProjeto.revisoesPendentes(pendentes, disciplinaIds),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RevisoesUiState(hoje.value),
    )

    fun atualizarData() {
        hoje.value = LocalDate.now(clock)
    }

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
                    clock = app.container.clock,
                )
            }
        }
    }
}
