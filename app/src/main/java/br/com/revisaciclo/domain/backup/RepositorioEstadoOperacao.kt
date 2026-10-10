package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Preferencias

/**
 * Marcador persistente de uma restauração iniciada, usado para concluí-la caso
 * o processo seja interrompido depois de o Room já ter sido substituído.
 *
 * Só existe estado quando há preferências pendentes de aplicação: a substituição
 * do Room é atômica e não precisa de marcador.
 */
data class EstadoOperacao(val preferenciasPendentes: Preferencias)

interface RepositorioEstadoOperacao {
    suspend fun ler(): EstadoOperacao?
    suspend fun gravar(estado: EstadoOperacao)
    suspend fun limpar()
}
