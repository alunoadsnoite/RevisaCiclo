package br.com.revisaciclo.ui.estatisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.agregacao.CalculadoraEstatisticas
import br.com.revisaciclo.domain.agregacao.FiltroProjeto
import br.com.revisaciclo.domain.model.EstatisticaRevisao
import br.com.revisaciclo.domain.projetos.observarProjetoAtivo
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.ProjetoRepository
import br.com.revisaciclo.domain.repository.RevisaoRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EstatisticasUiState(
    val carregando: Boolean = true,
    val estatisticas: EstatisticaRevisao? = null,
    val estatisticasGlobais: EstatisticaRevisao? = null,
)

class EstatisticasViewModel(
    private val repository: RevisaoRepository,
    private val preferenciasRepository: PreferenciasRepository,
    private val projetoRepository: ProjetoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val hoje = MutableStateFlow(LocalDate.now(clock))

    val uiState: StateFlow<EstatisticasUiState> = combine(
        hoje,
        observarProjetoAtivo(projetoRepository, preferenciasRepository),
        repository.observarDisciplinas(),
        repository.observarAssuntos(),
        repository.observarRevisoes(),
    ) { data, projeto, disciplinas, assuntos, revisoes ->
        val disciplinaIds = FiltroProjeto.disciplinas(disciplinas, projeto?.id)
            .map { it.id }
            .toSet()
        val assuntoIds = FiltroProjeto.assuntos(assuntos, disciplinaIds)
            .map { it.id }
            .toSet()
        val revisoesDoProjeto = FiltroProjeto.revisoes(revisoes, assuntoIds)

        EstatisticasUiState(
            carregando = false,
            estatisticas = CalculadoraEstatisticas.calcular(revisoesDoProjeto, data),
            estatisticasGlobais = CalculadoraEstatisticas.calcular(revisoes, data),
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstatisticasUiState(),
        )

    /**
     * Reavalia a data ao voltar ao primeiro plano (virada do dia ou troca de fuso).
     */
    fun atualizarData() {
        hoje.value = LocalDate.now(clock)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                EstatisticasViewModel(
                    repository = app.container.revisaoRepository,
                    preferenciasRepository = app.container.preferenciasRepository,
                    projetoRepository = app.container.projetoRepository,
                    clock = app.container.clock,
                )
            }
        }
    }
}
