package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import br.com.revisaciclo.domain.model.TemaApp
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Serialização e leitura do formato de backup `.revisaciclo`.
 *
 * O arquivo é um JSON com cabeçalho e `payload`. O checksum SHA-256 cobre a
 * serialização **canônica** do `payload` (chaves ordenadas, sem espaços), de modo
 * que a integridade seja verificável de forma determinística.
 */
object SerializadorBackup {

    private const val CAMPO_FORMATO = "formato"
    private const val CAMPO_VERSAO_FORMATO = "versaoFormato"
    private const val CAMPO_VERSAO_APP = "versaoApp"
    private const val CAMPO_VERSAO_SCHEMA = "versaoSchemaBanco"
    private const val CAMPO_CRIADO_EM = "criadoEm"
    private const val CAMPO_CHECKSUM = "checksum"
    private const val CAMPO_ALGORITMO = "algoritmo"
    private const val CAMPO_VALOR_CHECKSUM = "valor"
    private const val CAMPO_CONTAGENS = "contagens"
    private const val CAMPO_PAYLOAD = "payload"

    private const val COL_PROJETOS = "projetos"
    private const val COL_DISCIPLINAS = "disciplinas"
    private const val COL_ASSUNTOS = "assuntos"
    private const val COL_REVISOES = "revisoes"
    private const val COL_PREFERENCIAS = "preferencias"

    fun serializar(dados: BackupDados, versaoApp: String, agora: Instant): String {
        val payload = payloadParaJson(dados)
        val checksum = Checksum.sha256Hex(JsonEscritor.canonico(payload))

        val envelope = jsonObjeto {
            campo(CAMPO_FORMATO, BackupFormato.IDENTIFICADOR)
            campo(CAMPO_VERSAO_FORMATO, BackupFormato.VERSAO)
            campo(CAMPO_VERSAO_APP, versaoApp)
            campo(CAMPO_VERSAO_SCHEMA, BackupFormato.VERSAO_SCHEMA_BANCO)
            campo(CAMPO_CRIADO_EM, agora.toString())
            campo(
                CAMPO_CHECKSUM,
                jsonObjeto {
                    campo(CAMPO_ALGORITMO, Checksum.ALGORITMO)
                    campo(CAMPO_VALOR_CHECKSUM, checksum)
                },
            )
            campo(
                CAMPO_CONTAGENS,
                jsonObjeto {
                    campo(COL_PROJETOS, dados.projetos.size)
                    campo(COL_DISCIPLINAS, dados.disciplinas.size)
                    campo(COL_ASSUNTOS, dados.assuntos.size)
                    campo(COL_REVISOES, dados.revisoes.size)
                },
            )
            campo(CAMPO_PAYLOAD, payload)
        }
        return JsonEscritor.bonito(envelope)
    }

    fun serializarBytes(dados: BackupDados, versaoApp: String, agora: Instant): ByteArray =
        serializar(dados, versaoApp, agora).toByteArray(Charsets.UTF_8)

    fun desserializar(bytes: ByteArray): BackupDocumento {
        if (bytes.isEmpty()) {
            throw BackupInvalidoException(MotivoBackupInvalido.VAZIO, "O arquivo está vazio.")
        }
        if (bytes.size.toLong() > BackupFormato.LIMITE_BYTES) {
            throw BackupInvalidoException(
                MotivoBackupInvalido.GRANDE_DEMAIS,
                "O arquivo excede o limite de tamanho permitido.",
            )
        }
        return desserializar(String(bytes, Charsets.UTF_8))
    }

    fun desserializar(texto: String): BackupDocumento {
        val raiz = try {
            JsonLeitor.ler(texto, BackupFormato.PROFUNDIDADE_MAXIMA)
        } catch (erro: JsonInvalidoException) {
            throw BackupInvalidoException(MotivoBackupInvalido.JSON_INVALIDO, "O JSON é inválido.")
        }
        val envelope = raiz as? JsonObjeto
            ?: throw BackupInvalidoException(MotivoBackupInvalido.JSON_INVALIDO, "Raiz deve ser um objeto.")

        val formato = envelope.texto(CAMPO_FORMATO)
        if (formato != BackupFormato.IDENTIFICADOR) {
            throw BackupInvalidoException(
                MotivoBackupInvalido.FORMATO_DESCONHECIDO,
                "Este arquivo não é um backup do RevisaCiclo.",
            )
        }

        val versaoFormato = envelope.inteiro(CAMPO_VERSAO_FORMATO)
            ?: throw BackupInvalidoException(
                MotivoBackupInvalido.CAMPOS_AUSENTES,
                "Versão do formato ausente.",
            )
        if (versaoFormato > BackupFormato.VERSAO) {
            throw BackupInvalidoException(
                MotivoBackupInvalido.VERSAO_FUTURA,
                "Backup criado por uma versão mais nova do aplicativo.",
            )
        }
        if (versaoFormato < 1) {
            throw BackupInvalidoException(MotivoBackupInvalido.VALOR_INVALIDO, "Versão do formato inválida.")
        }

        val versaoSchema = envelope.inteiro(CAMPO_VERSAO_SCHEMA)
            ?: throw BackupInvalidoException(
                MotivoBackupInvalido.CAMPOS_AUSENTES,
                "Versão de schema ausente.",
            )
        if (versaoSchema > BackupFormato.VERSAO_SCHEMA_BANCO) {
            throw BackupInvalidoException(
                MotivoBackupInvalido.VERSAO_FUTURA,
                "Backup criado por uma versão mais nova do aplicativo.",
            )
        }

        val payloadJson = envelope[CAMPO_PAYLOAD] as? JsonObjeto
            ?: throw BackupInvalidoException(
                MotivoBackupInvalido.CAMPOS_AUSENTES,
                "Conteúdo do backup ausente.",
            )

        val checksumObj = envelope[CAMPO_CHECKSUM] as? JsonObjeto
            ?: throw BackupInvalidoException(
                MotivoBackupInvalido.CAMPOS_AUSENTES,
                "Metadados de integridade ausentes.",
            )
        val algoritmo = checksumObj.texto(CAMPO_ALGORITMO)
        val checksumEsperado = checksumObj.texto(CAMPO_VALOR_CHECKSUM)
        if (algoritmo != Checksum.ALGORITMO || checksumEsperado.isNullOrBlank()) {
            throw BackupInvalidoException(
                MotivoBackupInvalido.CHECKSUM_INCORRETO,
                "Metadados de integridade inválidos.",
            )
        }
        val checksumCalculado = Checksum.sha256Hex(JsonEscritor.canonico(payloadJson))
        if (!checksumCalculado.equals(checksumEsperado, ignoreCase = true)) {
            throw BackupInvalidoException(
                MotivoBackupInvalido.CHECKSUM_INCORRETO,
                "O checksum do backup não confere. O arquivo pode estar corrompido.",
            )
        }

        val versaoApp = envelope.texto(CAMPO_VERSAO_APP) ?: ""
        val criadoEm = envelope.texto(CAMPO_CRIADO_EM)?.let {
            try {
                Instant.parse(it)
            } catch (erro: DateTimeParseException) {
                throw BackupInvalidoException(
                    MotivoBackupInvalido.VALOR_INVALIDO,
                    "Data de criação inválida.",
                )
            }
        } ?: throw BackupInvalidoException(
            MotivoBackupInvalido.CAMPOS_AUSENTES,
            "Data de criação ausente.",
        )

        val dados = payloadParaDados(payloadJson)
        val contagens = envelope[CAMPO_CONTAGENS] as? JsonObjeto
        val cabecalho = BackupCabecalho(
            formato = formato,
            versaoFormato = versaoFormato.toInt(),
            versaoApp = versaoApp,
            versaoSchemaBanco = versaoSchema.toInt(),
            criadoEm = criadoEm,
            algoritmoChecksum = algoritmo,
            checksum = checksumEsperado,
            contagens = BackupContagens(
                projetos = contagens?.inteiro(COL_PROJETOS)?.toInt() ?: dados.projetos.size,
                disciplinas = contagens?.inteiro(COL_DISCIPLINAS)?.toInt() ?: dados.disciplinas.size,
                assuntos = contagens?.inteiro(COL_ASSUNTOS)?.toInt() ?: dados.assuntos.size,
                revisoes = contagens?.inteiro(COL_REVISOES)?.toInt() ?: dados.revisoes.size,
            ),
        )
        return BackupDocumento(cabecalho = cabecalho, dados = dados)
    }

    fun resumo(documento: BackupDocumento): ResumoBackup = ResumoBackup(
        projetos = documento.dados.projetos.size,
        disciplinas = documento.dados.disciplinas.size,
        assuntos = documento.dados.assuntos.size,
        revisoes = documento.dados.revisoes.size,
        versaoFormato = documento.cabecalho.versaoFormato,
        versaoApp = documento.cabecalho.versaoApp,
        criadoEm = documento.cabecalho.criadoEm,
    )

    // ---- payload -> Json ----------------------------------------------------

    private fun payloadParaJson(dados: BackupDados): JsonObjeto = jsonObjeto {
        campo(
            COL_PROJETOS,
            JsonLista(dados.projetos.map(::projetoParaJson)),
        )
        campo(
            COL_DISCIPLINAS,
            JsonLista(dados.disciplinas.map(::disciplinaParaJson)),
        )
        campo(
            COL_ASSUNTOS,
            JsonLista(dados.assuntos.map(::assuntoParaJson)),
        )
        campo(
            COL_REVISOES,
            JsonLista(dados.revisoes.map(::revisaoParaJson)),
        )
        campo(COL_PREFERENCIAS, preferenciasParaJson(dados.preferencias))
    }

    internal fun projetoParaJson(projeto: ProjetoEstudo): JsonObjeto = jsonObjeto {
        campo("id", projeto.id)
        campo("nome", projeto.nome)
        campoTextoOuNulo("descricao", projeto.descricao)
        campo("cor", projeto.cor)
        campo("criadoEm", projeto.criadoEm)
        campo("arquivado", projeto.arquivado)
    }

    internal fun disciplinaParaJson(disciplina: Disciplina): JsonObjeto = jsonObjeto {
        campo("id", disciplina.id)
        campo("projetoId", disciplina.projetoId)
        campo("nome", disciplina.nome)
        campo("cor", disciplina.cor)
        campo("criadaEm", disciplina.criadaEm)
    }

    internal fun assuntoParaJson(assunto: Assunto): JsonObjeto = jsonObjeto {
        campo("id", assunto.id)
        campo("disciplinaId", assunto.disciplinaId)
        campo("titulo", assunto.titulo)
        campoTextoOuNulo("descricao", assunto.descricao)
        campo("dataEstudo", assunto.dataEstudo.toString())
        campo("estadoRevisao", assunto.estadoRevisao.name)
        campo("indiceCiclo", assunto.indiceCiclo)
        campoTextoOuNulo("proximaRevisao", assunto.proximaRevisao?.toString())
    }

    internal fun revisaoParaJson(revisao: Revisao): JsonObjeto = jsonObjeto {
        campo("id", revisao.id)
        campo("assuntoId", revisao.assuntoId)
        campo("agendadaPara", revisao.agendadaPara.toString())
        campoTextoOuNulo("concluidaEm", revisao.concluidaEm?.toString())
        campo("status", revisao.status.name)
        campo("intervaloDias", revisao.intervaloDias)
        campo("indiceCiclo", revisao.indiceCiclo)
        campoTextoOuNulo("avaliacao", revisao.avaliacao?.name)
    }

    internal fun preferenciasParaJson(preferencias: Preferencias): JsonObjeto = jsonObjeto {
        campo("notificacoesAtivas", preferencias.notificacoesAtivas)
        campo("horarioHora", preferencias.horarioNotificacao.hour)
        campo("horarioMinuto", preferencias.horarioNotificacao.minute)
        campo("metaDiaria", preferencias.metaDiaria)
        campo("tema", preferencias.tema.name)
        campoInteiros("intervalosDias", preferencias.intervalosDias)
        campoInteiroOuNulo("projetoAtivoId", preferencias.projetoAtivoId)
    }

    // ---- Json -> payload ----------------------------------------------------

    private fun payloadParaDados(payload: JsonObjeto): BackupDados {
        val projetos = payload.colecao(COL_PROJETOS).map { item ->
            projetoDeJson(item.objetoObrigatorio("projeto"))
        }
        val disciplinas = payload.colecao(COL_DISCIPLINAS).map { item ->
            disciplinaDeJson(item.objetoObrigatorio("disciplina"))
        }
        val assuntos = payload.colecao(COL_ASSUNTOS).map { item ->
            assuntoDeJson(item.objetoObrigatorio("assunto"))
        }
        val revisoes = payload.colecao(COL_REVISOES).map { item ->
            revisaoDeJson(item.objetoObrigatorio("revisão"))
        }
        val preferenciasJson = payload[COL_PREFERENCIAS] as? JsonObjeto
            ?: throw BackupInvalidoException(
                MotivoBackupInvalido.CAMPOS_AUSENTES,
                "Preferências ausentes no backup.",
            )
        return BackupDados(
            projetos = projetos,
            disciplinas = disciplinas,
            assuntos = assuntos,
            revisoes = revisoes,
            preferencias = preferenciasDeJson(preferenciasJson),
        )
    }

    private fun projetoDeJson(obj: JsonObjeto): ProjetoEstudo = ProjetoEstudo(
        id = obj.inteiroObrigatorio("id", "projeto"),
        nome = obj.textoValido("nome", "projeto"),
        descricao = obj.textoOuNuloValidado("descricao", "projeto"),
        cor = obj.inteiroObrigatorio("cor", "projeto"),
        criadoEm = obj.inteiroObrigatorio("criadoEm", "projeto"),
        arquivado = obj.booleanoObrigatorio("arquivado", "projeto"),
    )

    private fun disciplinaDeJson(obj: JsonObjeto): Disciplina = Disciplina(
        id = obj.inteiroObrigatorio("id", "disciplina"),
        projetoId = obj.inteiroObrigatorio("projetoId", "disciplina"),
        nome = obj.textoValido("nome", "disciplina"),
        cor = obj.inteiroObrigatorio("cor", "disciplina"),
        criadaEm = obj.inteiroObrigatorio("criadaEm", "disciplina"),
    )

    private fun assuntoDeJson(obj: JsonObjeto): Assunto = Assunto(
        id = obj.inteiroObrigatorio("id", "assunto"),
        disciplinaId = obj.inteiroObrigatorio("disciplinaId", "assunto"),
        titulo = obj.textoValido("titulo", "assunto"),
        descricao = obj.textoOuNuloValidado("descricao", "assunto"),
        dataEstudo = obj.dataObrigatoria("dataEstudo", "assunto"),
        estadoRevisao = obj.enumObrigatorio("estadoRevisao", EstadoRevisao.entries, "assunto"),
        indiceCiclo = obj.inteiroObrigatorio("indiceCiclo", "assunto").toInt(),
        proximaRevisao = obj.dataOuNula("proximaRevisao", "assunto"),
    )

    private fun revisaoDeJson(obj: JsonObjeto): Revisao = Revisao(
        id = obj.inteiroObrigatorio("id", "revisão"),
        assuntoId = obj.inteiroObrigatorio("assuntoId", "revisão"),
        agendadaPara = obj.dataObrigatoria("agendadaPara", "revisão"),
        concluidaEm = obj.dataOuNula("concluidaEm", "revisão"),
        status = obj.enumObrigatorio("status", StatusRevisao.entries, "revisão"),
        intervaloDias = obj.inteiroObrigatorio("intervaloDias", "revisão").toInt(),
        indiceCiclo = obj.inteiroObrigatorio("indiceCiclo", "revisão").toInt(),
        avaliacao = obj.enumOuNuloObrigatorio("avaliacao", AvaliacaoRevisao.entries, "revisão"),
    )

    private fun preferenciasDeJson(obj: JsonObjeto): Preferencias {
        val hora = obj.inteiroObrigatorio("horarioHora", "preferências").toInt()
        val minuto = obj.inteiroObrigatorio("horarioMinuto", "preferências").toInt()
        if (hora !in 0..23 || minuto !in 0..59) {
            throw BackupInvalidoException(MotivoBackupInvalido.VALOR_INVALIDO, "Horário inválido.")
        }
        val intervalos = obj.colecao("intervalosDias").map { item ->
            (item as? JsonInteiro)?.valor
                ?: throw BackupInvalidoException(
                    MotivoBackupInvalido.VALOR_INVALIDO,
                    "Intervalos inválidos.",
                )
        }.map { it.toInt() }
        return Preferencias(
            notificacoesAtivas = obj.booleanoObrigatorio("notificacoesAtivas", "preferências"),
            horarioNotificacao = java.time.LocalTime.of(hora, minuto),
            metaDiaria = obj.inteiroObrigatorio("metaDiaria", "preferências").toInt(),
            tema = obj.enumObrigatorio("tema", TemaApp.entries, "preferências"),
            intervalosDias = intervalos,
            projetoAtivoId = obj["projetoAtivoId"]?.let { valor ->
                if (valor.ehNulo) {
                    null
                } else {
                    (valor as? JsonInteiro)?.valor
                        ?: throw BackupInvalidoException(
                            MotivoBackupInvalido.VALOR_INVALIDO,
                            "Projeto ativo inválido.",
                        )
                }
            },
        )
    }

    // ---- auxiliares ---------------------------------------------------------

    private fun JsonObjeto.colecao(campo: String): List<JsonValor> {
        val lista = this[campo] as? JsonLista
            ?: throw BackupInvalidoException(
                MotivoBackupInvalido.CAMPOS_AUSENTES,
                "Coleção \"$campo\" ausente.",
            )
        if (lista.itens.size > BackupFormato.LIMITE_REGISTROS) {
            throw BackupInvalidoException(
                MotivoBackupInvalido.GRANDE_DEMAIS,
                "Há registros demais no backup.",
            )
        }
        return lista.itens
    }

    private fun JsonValor.objetoObrigatorio(descricao: String): JsonObjeto =
        this as? JsonObjeto
            ?: throw BackupInvalidoException(
                MotivoBackupInvalido.VALOR_INVALIDO,
                "Registro de $descricao inválido.",
            )

    private fun JsonObjeto.inteiro(campo: String): Long? {
        val valor = this[campo] ?: return null
        return when (valor) {
            is JsonInteiro -> valor.valor
            JsonNulo -> null
            else -> throw BackupInvalidoException(
                MotivoBackupInvalido.VALOR_INVALIDO,
                "Campo \"$campo\" deve ser inteiro.",
            )
        }
    }

    private fun JsonObjeto.texto(campo: String): String? {
        val valor = this[campo] ?: return null
        return when (valor) {
            is JsonTexto -> valor.valor
            JsonNulo -> null
            else -> throw BackupInvalidoException(
                MotivoBackupInvalido.VALOR_INVALIDO,
                "Campo \"$campo\" deve ser texto.",
            )
        }
    }

    private fun JsonObjeto.inteiroObrigatorio(campo: String, descricao: String): Long =
        inteiro(campo) ?: throw BackupInvalidoException(
            MotivoBackupInvalido.CAMPOS_AUSENTES,
            "Campo \"$campo\" ausente no $descricao.",
        )

    private fun JsonObjeto.booleanoObrigatorio(campo: String, descricao: String): Boolean {
        val valor = this[campo] ?: throw BackupInvalidoException(
            MotivoBackupInvalido.CAMPOS_AUSENTES,
            "Campo \"$campo\" ausente no $descricao.",
        )
        return (valor as? JsonBooleano)?.valor ?: throw BackupInvalidoException(
            MotivoBackupInvalido.VALOR_INVALIDO,
            "Campo \"$campo\" deve ser booleano.",
        )
    }

    private fun JsonObjeto.textoValido(campo: String, descricao: String): String {
        val valor = texto(campo) ?: throw BackupInvalidoException(
            MotivoBackupInvalido.CAMPOS_AUSENTES,
            "Campo \"$campo\" ausente no $descricao.",
        )
        if (valor.length > BackupFormato.LIMITE_TEXTO) {
            throw BackupInvalidoException(MotivoBackupInvalido.VALOR_INVALIDO, "Texto muito longo.")
        }
        return valor
    }

    private fun JsonObjeto.textoOuNuloValidado(campo: String, descricao: String): String? {
        val valor = texto(campo) ?: return null
        if (valor.length > BackupFormato.LIMITE_TEXTO) {
            throw BackupInvalidoException(MotivoBackupInvalido.VALOR_INVALIDO, "Texto muito longo.")
        }
        return valor
    }

    private fun JsonObjeto.dataObrigatoria(campo: String, descricao: String): LocalDate {
        val texto = texto(campo) ?: throw BackupInvalidoException(
            MotivoBackupInvalido.CAMPOS_AUSENTES,
            "Campo \"$campo\" ausente no $descricao.",
        )
        return try {
            LocalDate.parse(texto)
        } catch (erro: DateTimeParseException) {
            throw BackupInvalidoException(MotivoBackupInvalido.VALOR_INVALIDO, "Data \"$campo\" inválida.")
        }
    }

    private fun JsonObjeto.dataOuNula(campo: String, descricao: String): LocalDate? {
        if (this[campo] == null || this[campo].ehNulo) return null
        return dataObrigatoria(campo, descricao)
    }

    private fun <T : Enum<T>> JsonObjeto.enumObrigatorio(
        campo: String,
        valores: List<T>,
        descricao: String,
    ): T {
        val texto = texto(campo) ?: throw BackupInvalidoException(
            MotivoBackupInvalido.CAMPOS_AUSENTES,
            "Campo \"$campo\" ausente no $descricao.",
        )
        return valores.firstOrNull { it.name == texto }
            ?: throw BackupInvalidoException(
                MotivoBackupInvalido.VALOR_INVALIDO,
                "Valor inválido em \"$campo\".",
            )
    }

    private fun <T : Enum<T>> JsonObjeto.enumOuNuloObrigatorio(
        campo: String,
        valores: List<T>,
        descricao: String,
    ): T? {
        if (this[campo] == null || this[campo].ehNulo) return null
        return enumObrigatorio(campo, valores, descricao)
    }
}
