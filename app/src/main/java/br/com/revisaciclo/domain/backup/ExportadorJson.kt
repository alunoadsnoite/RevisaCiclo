package br.com.revisaciclo.domain.backup

import java.time.Instant

/**
 * Exportação portátil em JSON aberto, pensada para interoperabilidade e leitura
 * por outras ferramentas. Diferente do backup `.revisaciclo`, **não** inclui
 * checksum nem envelope de aplicativo; as chaves de topo são estáveis e
 * documentadas em `docs/BACKUP_FORMAT.md`.
 */
object ExportadorJson {

    const val SCHEMA_VERSION = 1

    private const val CAMPO_SCHEMA = "schemaVersion"
    private const val CAMPO_EXPORTADO_EM = "exportedAt"
    private const val CAMPO_VERSAO_APP = "appVersion"
    private const val CAMPO_PROJETOS = "projects"
    private const val CAMPO_DISCIPLINAS = "disciplines"
    private const val CAMPO_ASSUNTOS = "topics"
    private const val CAMPO_REVISOES = "reviews"
    private const val CAMPO_PREFERENCIAS = "preferences"

    fun exportar(dados: BackupDados, versaoApp: String, agora: Instant): String {
        val raiz = jsonObjeto {
            campo(CAMPO_SCHEMA, SCHEMA_VERSION)
            campo(CAMPO_EXPORTADO_EM, agora.toString())
            campo(CAMPO_VERSAO_APP, versaoApp)
            campo(CAMPO_PROJETOS, JsonLista(dados.projetos.map(SerializadorBackup::projetoParaJson)))
            campo(
                CAMPO_DISCIPLINAS,
                JsonLista(dados.disciplinas.map(SerializadorBackup::disciplinaParaJson)),
            )
            campo(CAMPO_ASSUNTOS, JsonLista(dados.assuntos.map(SerializadorBackup::assuntoParaJson)))
            campo(CAMPO_REVISOES, JsonLista(dados.revisoes.map(SerializadorBackup::revisaoParaJson)))
            campo(CAMPO_PREFERENCIAS, SerializadorBackup.preferenciasParaJson(dados.preferencias))
        }
        return JsonEscritor.bonito(raiz)
    }

    fun exportarBytes(dados: BackupDados, versaoApp: String, agora: Instant): ByteArray =
        exportar(dados, versaoApp, agora).toByteArray(Charsets.UTF_8)
}
