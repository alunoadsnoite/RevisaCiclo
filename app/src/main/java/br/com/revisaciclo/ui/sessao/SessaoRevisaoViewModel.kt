package br.com.revisaciclo.ui.sessao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.domain.repository.RevisaoRepository
import br.com.revisaciclo.domain.sessao.SessaoDeRevisao
import br.com.revisaciclo.domain.sessao.avaliarProximo
import br.com.revisaciclo.domain.sessao.revisoesDevidas
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SessaoRevisaoUiState(
    val carregando: Boolean = true,
    val sessao: SessaoDeRevisao? = null,
    val processando: Boolean = false,
    val erro: String? = null,
) {
    val vazia: Boolean
        get() = !carregando && (sessao?.total ?: 0) == 0

    val concluida: Boolean
        get() = !carregando && !vazia && (sessao?.concluida == true)

    val atual: RevisaoPendente?
        get() = sessao?.atual
}

internal fun podeAvaliar(estado: SessaoRevisaoUiState): Boolean =
    !estado.carregando && !estado.processando && !estado.concluida && !estado.vazia && estado.atual != null

class SessaoRevisaoViewModel(
    private val repository: RevisaoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessaoRevisaoUiState())
    val uiState: StateFlow<SessaoRevisaoUiState> = _uiState.asStateFlow()

    init {
        carregarDevidas()
    }

    fun avaliar(avaliacao: AvaliacaoRevisao) {
        val estado = _uiState.value
        if (!podeAvaliar(estado)) return
        val sessao = estado.sessao ?: return

        _uiState.value = estado.copy(processando = true, erro = null)
        viewModelScope.launch {
            try {
                avaliarProximo(
                    sessao = sessao,
                    persistir = { atual, avaliacaoAtual ->
                        repository.concluirRevisao(atual.revisaoId, avaliacaoAtual)
                    },
                    avaliacao = avaliacao,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    erro = "Não foi possível salvar a avaliação. Tente novamente.",
                )
            } finally {
                val estadoApos = _uiState.value
                _uiState.value = estadoApos.copy(processando = false)
            }
        }
    }

    /**
     * Reavalia a data atual ao voltar ao primeiro plano (virada do dia ou troca de fuso).
     * Só recompõe a fila se a sessão ainda não tiver avaliado nenhum item,
     * preservando o progresso de uma sessão em andamento.
     */
    fun atualizarData() {
        val estadoAtual = _uiState.value
        if (estadoAtual.carregando || estadoAtual.processando) return
        val sessaoAtual = estadoAtual.sessao
        if (sessaoAtual != null && sessaoAtual.concluidas > 0) return
        carregarDevidas()
    }

    private fun carregarDevidas() {
        viewModelScope.launch {
            _uiState.value = SessaoRevisaoUiState(carregando = true)
            val hoje = LocalDate.now(clock)
            val pendentes = repository.observarRevisoesPendentes().first()
            val devidas = revisoesDevidas(pendentes, hoje)
            _uiState.value = SessaoRevisaoUiState(
                carregando = false,
                sessao = SessaoDeRevisao(devidas),
            )
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                SessaoRevisaoViewModel(app.container.revisaoRepository, app.container.clock)
            }
        }
    }
}