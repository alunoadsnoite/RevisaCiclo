package br.com.revisaciclo.ui.roda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.agregacao.CalculadoraRoda
import br.com.revisaciclo.domain.model.RodaRevisao
import br.com.revisaciclo.domain.repository.RevisaoRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RodaRevisaoUiState(
    val carregando: Boolean = true,
    val roda: RodaRevisao? = null,
    val estagioSelecionado: Int? = null,
) {
    val estaVazia: Boolean
        get() = !carregando && (roda?.estaVazia == true)
}

class RodaRevisaoViewModel(
    private val repository: RevisaoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RodaRevisaoUiState())
    val uiState: StateFlow<RodaRevisaoUiState> = _uiState.asStateFlow()

    private val hoje = MutableStateFlow(LocalDate.now(clock))

    init {
        viewModelScope.launch {
            combine(
                hoje,
                repository.observarAssuntos(),
                repository.observarDisciplinas(),
            ) { data, assuntos, disciplinas ->
                val porId = disciplinas.associateBy { it.id }
                CalculadoraRoda.montar(assuntos, porId, data)
            }.collect { roda ->
                val atual = _uiState.value
                val valido = atual.estagioSelecionado
                    ?.let { selecionado -> roda.estagios.any { it.intervaloDias == selecionado && it.total > 0 } }
                    ?: false
                _uiState.update { estado ->
                    estado.copy(
                        carregando = false,
                        roda = roda,
                        estagioSelecionado = if (valido) {
                            atual.estagioSelecionado
                        } else {
                            roda.estagios.firstOrNull { it.total > 0 }?.intervaloDias
                        },
                    )
                }
            }
        }
    }

    fun selecionarEstagio(intervaloDias: Int) {
        _uiState.update { it.copy(estagioSelecionado = intervaloDias) }
    }

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
                RodaRevisaoViewModel(app.container.revisaoRepository, app.container.clock)
            }
        }
    }
}