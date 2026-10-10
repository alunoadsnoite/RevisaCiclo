package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Preferencias

/**
 * Acesso aos dados persistentes para backup/restauração.
 *
 * A leitura de Room é feita em uma transação de leitura para obter um snapshot
 * consistente entre as tabelas. As preferências (DataStore) são lidas à parte:
 * **não há transação conjunta entre Room e DataStore**, então a consistência
 * entre os dois é tratada pelo protocolo de restauração, não pelo armazenamento.
 */
interface ArmazenamentoBackup {

    /** Snapshot consistente de todas as tabelas + preferências. */
    suspend fun capturar(): BackupDados

    /**
     * Substitui todo o conteúdo do Room por [dados], dentro de **uma única
     * transação**. Em caso de falha, o Room é revertido automaticamente.
     * As preferências NÃO são tocadas aqui.
     */
    suspend fun substituirRoom(dados: BackupDados)

    suspend fun aplicarPreferencias(preferencias: Preferencias)

    suspend fun lerPreferencias(): Preferencias
}
