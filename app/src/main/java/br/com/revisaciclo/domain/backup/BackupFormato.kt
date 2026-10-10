package br.com.revisaciclo.domain.backup

/**
 * Constantes e limites do formato de backup do RevisaCiclo.
 *
 * O arquivo é texto JSON UTF-8 com extensão [EXTENSAO]. Ele não é criptografado
 * nesta versão: pode conter dados pessoais de estudo.
 */
object BackupFormato {

    /** Identificador do formato gravado no cabeçalho. */
    const val IDENTIFICADOR = "revisaciclo-backup"

    /** Versão do formato de backup suportada por esta versão do aplicativo. */
    const val VERSAO = 1

    /** Versão do schema Room que o aplicativo atual grava/espera. */
    const val VERSAO_SCHEMA_BANCO = 3

    const val EXTENSAO = ".revisaciclo"

    const val MIME = "application/json"

    /** Limite de leitura do arquivo, para evitar consumo excessivo de memória. */
    const val LIMITE_BYTES = 25L * 1024 * 1024

    /** Profundidade máxima de aninhamento aceita no JSON. */
    const val PROFUNDIDADE_MAXIMA = 32

    /** Número máximo de registros por coleção. */
    const val LIMITE_REGISTROS = 200_000

    /** Tamanho máximo de um texto (nome, título, descrição). */
    const val LIMITE_TEXTO = 10_000

    fun nomeSugerido(ano: Int, mes: Int, dia: Int): String =
        "revisaciclo-backup-%04d%02d%02d%s".format(ano, mes, dia, EXTENSAO)
}
