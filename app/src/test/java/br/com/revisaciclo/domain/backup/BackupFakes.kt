package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Preferencias
import java.io.IOException

class ArmazenamentoBackupFake(
    var dados: BackupDados = BackupDados(
        projetos = emptyList(),
        disciplinas = emptyList(),
        assuntos = emptyList(),
        revisoes = emptyList(),
        preferencias = Preferencias(),
    ),
    var falharCapturar: Throwable? = null,
    var falharSubstituir: Throwable? = null,
    var falharAplicar: Throwable? = null,
) : ArmazenamentoBackup {
    var substituicoes = 0
    var prefsAplicadas: Preferencias? = null

    override suspend fun capturar(): BackupDados {
        falharCapturar?.let { throw it }
        return dados
    }

    override suspend fun substituirRoom(dados: BackupDados) {
        substituicoes++
        falharSubstituir?.let { throw it }
        this.dados = dados
    }

    override suspend fun aplicarPreferencias(preferencias: Preferencias) {
        falharAplicar?.let { throw it }
        prefsAplicadas = preferencias
        dados = dados.copy(preferencias = preferencias)
    }

    override suspend fun lerPreferencias(): Preferencias = dados.preferencias
}

class EstadoOperacaoFake : RepositorioEstadoOperacao {
    var estado: EstadoOperacao? = null
    var gravacoes = 0
    var limpezas = 0

    override suspend fun ler(): EstadoOperacao? = estado

    override suspend fun gravar(estado: EstadoOperacao) {
        gravacoes++
        this.estado = estado
    }

    override suspend fun limpar() {
        limpezas++
        estado = null
    }
}

class FonteArquivoFake(
    var conteudo: ByteArray? = null,
    var falha: Throwable? = null,
) : FonteArquivo {
    var ultimoIdentificadorLido: String? = null
    var ultimoIdentificadorEscrito: String? = null
    var escrito: ByteArray? = null

    override suspend fun ler(identificador: String, limiteBytes: Long): ByteArray {
        ultimoIdentificadorLido = identificador
        falha?.let { throw it }
        return conteudo ?: throw IOException("Conteúdo ausente.")
    }

    override suspend fun escrever(identificador: String, conteudo: ByteArray) {
        falha?.let { throw it }
        ultimoIdentificadorEscrito = identificador
        escrito = conteudo
    }
}

class ArmazenamentoSegurancaFake : ArmazenamentoSeguranca {
    var salvo: ByteArray? = null
    var apagado: String? = null
    var identificador: String = "seguranca-fake.revisaciclo"

    override suspend fun salvar(conteudo: ByteArray): String {
        salvo = conteudo
        return identificador
    }

    override suspend fun apagar(identificador: String) {
        apagado = identificador
    }
}
