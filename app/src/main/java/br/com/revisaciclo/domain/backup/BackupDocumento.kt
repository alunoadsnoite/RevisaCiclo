package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.model.Revisao
import java.time.Instant

/** Motivo tipado de um arquivo de backup inválido. */
enum class MotivoBackupInvalido {
    VAZIO,
    GRANDE_DEMAIS,
    JSON_INVALIDO,
    FORMATO_DESCONHECIDO,
    VERSAO_FUTURA,
    CHECKSUM_INCORRETO,
    CAMPOS_AUSENTES,
    VALOR_INVALIDO,
}

class BackupInvalidoException(
    val motivo: MotivoBackupInvalido,
    mensagem: String,
) : Exception(mensagem)

/** Contagens presentes no cabeçalho, usadas para prévia e validação cruzada. */
data class BackupContagens(
    val projetos: Int,
    val disciplinas: Int,
    val assuntos: Int,
    val revisoes: Int,
)

data class BackupCabecalho(
    val formato: String,
    val versaoFormato: Int,
    val versaoApp: String,
    val versaoSchemaBanco: Int,
    val criadoEm: Instant,
    val algoritmoChecksum: String,
    val checksum: String,
    val contagens: BackupContagens,
)

/** Conteúdo restaurado, em modelos de domínio (independente do Room). */
data class BackupDados(
    val projetos: List<ProjetoEstudo>,
    val disciplinas: List<Disciplina>,
    val assuntos: List<Assunto>,
    val revisoes: List<Revisao>,
    val preferencias: Preferencias,
)

data class BackupDocumento(
    val cabecalho: BackupCabecalho,
    val dados: BackupDados,
)

/** Resumo apresentado ao usuário antes de confirmar a restauração. */
data class ResumoBackup(
    val projetos: Int,
    val disciplinas: Int,
    val assuntos: Int,
    val revisoes: Int,
    val versaoFormato: Int,
    val versaoApp: String,
    val criadoEm: Instant,
)
