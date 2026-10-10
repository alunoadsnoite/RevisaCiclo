package br.com.revisaciclo.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.agregacao.CalculadoraMeta
import br.com.revisaciclo.domain.agregacao.FiltroProjeto
import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.ProgressoMeta
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
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

data class DashboardUiState(
    val carregando: Boolean = true,
    val totalDisciplinas: Int = 0,
    val totalAssuntos: Int = 0,
    val totalRevisoes: Int = 0,
    val revisoesConcluidas: Int = 0,
    val proximaRevisao: Revisao? = null,
    val progressos: List<ProgressoDisciplina> = emptyList(),
    val progressoMeta: ProgressoMeta = ProgressoMeta(0, 20),
) {
    val revisoesPendentes: Int
        get() = (totalRevisoes - revisoesConcluidas).coerceAtLeast(0)

    val progressoGeral: Float
        get() = if (totalRevisoes == 0) 0f else revisoesConcluidas.toFloat() / totalRevisoes
}

private data class DadosDashboard(
    val disciplinas: List<Disciplina>,
    val assuntos: List<Assunto>,
    val revisoes: List<Revisao>,
    val progressos: List<ProgressoDisciplina>,
)

class DashboardViewModel(
    private val repository: RevisaoRepository,
    private val preferenciasRepository: PreferenciasRepository,
    private val projetoRepository: ProjetoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val hoje = MutableStateFlow(LocalDate.now(clock))

    private val dados = combine(
        repository.observarDisciplinas(),
        repository.observarAssuntos(),
        repository.observarRevisoes(),
        repository.observarProgressoPorDisciplina(),
    ) { disciplinas, assuntos, revisoes, progressos ->
        DadosDashboard(disciplinas, assuntos, revisoes, progressos)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        hoje,
        preferenciasRepository.preferencias,
        observarProjetoAtivo(projetoRepository, preferenciasRepository),
        dados,
    ) { data, preferencias, projeto, dados ->
        val disciplinas = FiltroProjeto.disciplinas(dados.disciplinas, projeto?.id)
        val disciplinaIds = disciplinas.map { it.id }.toSet()
        val assuntos = FiltroProjeto.assuntos(dados.assuntos, disciplinaIds)
        val assuntoIds = assuntos.map { it.id }.toSet()
        val revisoes = FiltroProjeto.revisoes(dados.revisoes, assuntoIds)
        val progressos = FiltroProjeto.progresso(dados.progressos, disciplinaIds)

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
            progressoMeta = CalculadoraMeta.calcular(
                revisoes = revisoes,
                hoje = data,
                metaDiaria = preferencias.metaDiaria,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
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
                DashboardViewModel(
                    repository = app.container.revisaoRepository,
                    preferenciasRepository = app.container.preferenciasRepository,
                    projetoRepository = app.container.projetoRepository,
                    clock = app.container.clock,
                )
            }
        }
    }
}
