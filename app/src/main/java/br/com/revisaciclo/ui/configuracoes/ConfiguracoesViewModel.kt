package br.com.revisaciclo.ui.configuracoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.TemaApp
import br.com.revisaciclo.domain.notificacao.GerenciadorNotificacoes
import br.com.revisaciclo.domain.preferencias.ResultadoIntervalos
import br.com.revisaciclo.domain.preferencias.ValidadorIntervalos
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.scheduler.MotorRevisaoEspacada
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfiguracoesUiState(
    val carregando: Boolean = true,
    val preferencias: Preferencias = Preferencias(),
    val erroIntervalos: String? = null,
)

class ConfiguracoesViewModel(
    private val preferenciasRepository: PreferenciasRepository,
    private val gerenciadorNotificacoes: GerenciadorNotificacoes,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracoesUiState())
    val uiState: StateFlow<ConfiguracoesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferenciasRepository.preferencias.collect { preferencias ->
                _uiState.update { it.copy(carregando = false, preferencias = preferencias) }
            }
        }
    }

    fun definirNotificacoesAtivas(ativo: Boolean) {
        viewModelScope.launch {
            preferenciasRepository.atualizar { it.copy(notificacoesAtivas = ativo) }
            gerenciadorNotificacoes.sincronizar(preferenciasRepository.preferencias.first())
        }
    }

    fun definirHorario(hora: Int, minuto: Int) {
        val horario = LocalTime.of(hora.coerceIn(0, 23), minuto.coerceIn(0, 59))
        viewModelScope.launch {
            preferenciasRepository.atualizar { it.copy(horarioNotificacao = horario) }
            gerenciadorNotificacoes.sincronizar(preferenciasRepository.preferencias.first())
        }
    }

    fun definirMetaDiaria(valor: Int) {
        val ajustada = valor.coerceIn(
            Preferencias.META_DIARIA_MINIMA,
            Preferencias.META_DIARIA_MAXIMA,
        )
        viewModelScope.launch {
            preferenciasRepository.atualizar { it.copy(metaDiaria = ajustada) }
        }
    }

    fun definirTema(tema: TemaApp) {
        viewModelScope.launch {
            preferenciasRepository.atualizar { it.copy(tema = tema) }
        }
    }

    fun salvarIntervalos(texto: String): Boolean {
        return when (val resultado = ValidadorIntervalos.deTexto(texto)) {
            is ResultadoIntervalos.Valido -> {
                viewModelScope.launch {
                    preferenciasRepository.atualizar { it.copy(intervalosDias = resultado.intervalos) }
                    _uiState.update { it.copy(erroIntervalos = null) }
                }
                true
            }

            is ResultadoIntervalos.Invalido -> {
                _uiState.update { it.copy(erroIntervalos = resultado.motivo.mensagem) }
                false
            }
        }
    }

    fun restaurarIntervalosPadrao() {
        viewModelScope.launch {
            preferenciasRepository.atualizar {
                it.copy(intervalosDias = MotorRevisaoEspacada.INTERVALOS_PADRAO)
            }
            _uiState.update { it.copy(erroIntervalos = null) }
        }
    }

    fun limparErroIntervalos() {
        _uiState.update { it.copy(erroIntervalos = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                ConfiguracoesViewModel(
                    preferenciasRepository = app.container.preferenciasRepository,
                    gerenciadorNotificacoes = GerenciadorNotificacoes(app.container.agendadorNotificacoes),
                )
            }
        }
    }
}
