package br.com.revisaciclo.ui.configuracoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.data.backup.PublicadorArquivo
import br.com.revisaciclo.domain.backup.BackupDocumento
import br.com.revisaciclo.domain.backup.BackupFormato
import br.com.revisaciclo.domain.backup.ResumoBackup
import br.com.revisaciclo.domain.backup.ResultadoLeituraBackup
import br.com.revisaciclo.domain.backup.ResultadoRestauracao
import br.com.revisaciclo.domain.backup.ServicoBackup
import br.com.revisaciclo.domain.backup.ServicoRestauracao
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Prévia validada aguardando confirmação do usuário. */
data class PreviaRestauracao(
    val resumo: ResumoBackup,
    val documento: BackupDocumento,
)

data class BackupUiState(
    val exportando: Boolean = false,
    val analisando: Boolean = false,
    val restaurando: Boolean = false,
    val previa: PreviaRestauracao? = null,
    val mensagem: String? = null,
    val compartilhar: String? = null,
) {
    val ocupado: Boolean get() = exportando || analisando || restaurando
}

class BackupViewModel(
    private val servicoBackup: ServicoBackup,
    private val servicoRestauracao: ServicoRestauracao,
    private val publicadorArquivo: PublicadorArquivo,
    private val clock: Clock,
) : ViewModel() {

    val hoje: LocalDate
        get() = LocalDate.now(clock)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportarBackup(uri: String) {
        if (_uiState.value.ocupado) return
        _uiState.update { it.copy(exportando = true, mensagem = null) }
        viewModelScope.launch {
            try {
                servicoBackup.salvarBackup(uri)
                _uiState.update { it.copy(mensagem = "Backup salvo com sucesso.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(mensagem = "Não foi possível salvar o backup.") }
            } finally {
                _uiState.update { it.copy(exportando = false) }
            }
        }
    }

    fun exportarJson(uri: String) {
        if (_uiState.value.ocupado) return
        _uiState.update { it.copy(exportando = true, mensagem = null) }
        viewModelScope.launch {
            try {
                servicoBackup.salvarJson(uri)
                _uiState.update { it.copy(mensagem = "Arquivo JSON exportado.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(mensagem = "Não foi possível exportar o JSON.") }
            } finally {
                _uiState.update { it.copy(exportando = false) }
            }
        }
    }

    fun compartilharBackup() {
        if (_uiState.value.ocupado) return
        _uiState.update { it.copy(exportando = true, mensagem = null) }
        viewModelScope.launch {
            try {
                val bytes = servicoBackup.criarBackupBytes()
                val data = hoje
                val nome = BackupFormato.nomeSugerido(data.year, data.monthValue, data.dayOfMonth)
                val uri = publicadorArquivo.publicar(nome, bytes)
                _uiState.update { it.copy(exportando = false, compartilhar = uri) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        exportando = false,
                        mensagem = "Não foi possível preparar o arquivo para compartilhar.",
                    )
                }
            }
        }
    }

    fun consumirCompartilhamento() {
        _uiState.update { it.copy(compartilhar = null) }
    }

    fun analisarBackup(uri: String) {
        if (_uiState.value.ocupado) return
        _uiState.update { it.copy(analisando = true, mensagem = null) }
        viewModelScope.launch {
            val resultado = servicoRestauracao.lerArquivo(uri)
            _uiState.update {
                when (resultado) {
                    is ResultadoLeituraBackup.Pronto -> it.copy(
                        analisando = false,
                        previa = PreviaRestauracao(resultado.resumo, resultado.documento),
                    )

                    is ResultadoLeituraBackup.Invalido ->
                        it.copy(analisando = false, mensagem = resultado.mensagem)
                }
            }
        }
    }

    fun confirmarRestauracao() {
        val previa = _uiState.value.previa ?: return
        if (_uiState.value.ocupado) return
        _uiState.update { it.copy(restaurando = true, previa = null, mensagem = null) }
        viewModelScope.launch {
            val resultado = servicoRestauracao.restaurar(previa.documento)
            val mensagem = when (resultado) {
                is ResultadoRestauracao.Sucesso ->
                    "Dados restaurados: ${resultado.resumo.assuntos} assuntos, " +
                        "${resultado.resumo.revisoes} revisões."

                is ResultadoRestauracao.Falha -> resultado.mensagem
            }
            _uiState.update { it.copy(restaurando = false, mensagem = mensagem) }
        }
    }

    fun descartarPrevia() {
        _uiState.update { it.copy(previa = null) }
    }

    fun limparMensagem() {
        _uiState.update { it.copy(mensagem = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RevisaCicloApp
                BackupViewModel(
                    servicoBackup = app.container.servicoBackup,
                    servicoRestauracao = app.container.servicoRestauracao,
                    publicadorArquivo = app.container.publicadorArquivo,
                    clock = app.container.clock,
                )
            }
        }
    }
}
