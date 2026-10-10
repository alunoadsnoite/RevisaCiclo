package br.com.revisaciclo.domain.backup

/**
 * Modelo JSON mínimo, sequencial e determinístico, escrito à mão para evitar
 * dependências externas de serialização.
 *
 * Convenções:
 * - Só há números inteiros ([JsonInteiro]); decimais e expoentes são rejeitados.
 * - A escrita canônica ordena as chaves de objeto alfabeticamente e não usa
 *   espaços, garantindo bytes estáveis para o cálculo do checksum.
 * - O leitor limita a profundidade de aninhamento; o tamanho é limitado antes
 *   da leitura.
 */
sealed interface JsonValor

data class JsonObjeto(val campos: Map<String, JsonValor>) : JsonValor {
    operator fun get(chave: String): JsonValor? = campos[chave]
}

data class JsonLista(val itens: List<JsonValor>) : JsonValor

data class JsonTexto(val valor: String) : JsonValor

data class JsonInteiro(val valor: Long) : JsonValor

data class JsonBooleano(val valor: Boolean) : JsonValor

data object JsonNulo : JsonValor

class JsonInvalidoException(mensagem: String) : Exception(mensagem)

fun JsonValor?.comoObjeto(): JsonObjeto? = this as? JsonObjeto

fun JsonValor?.comoLista(): JsonLista? = this as? JsonLista

fun JsonValor?.comoTexto(): String? = (this as? JsonTexto)?.valor

fun JsonValor?.comoInteiro(): Long? = (this as? JsonInteiro)?.valor

fun JsonValor?.comoBooleano(): Boolean? = (this as? JsonBooleano)?.valor

val JsonValor?.ehNulo: Boolean get() = this == null || this is JsonNulo

class JsonObjetoBuilder {
    private val campos = linkedMapOf<String, JsonValor>()

    fun campo(chave: String, valor: JsonValor) {
        campos[chave] = valor
    }

    fun campo(chave: String, valor: String) = campo(chave, JsonTexto(valor))

    fun campo(chave: String, valor: Long) = campo(chave, JsonInteiro(valor))

    fun campo(chave: String, valor: Int) = campo(chave, JsonInteiro(valor.toLong()))

    fun campo(chave: String, valor: Boolean) = campo(chave, JsonBooleano(valor))

    fun campoNulo(chave: String) = campo(chave, JsonNulo)

    fun campoTextoOuNulo(chave: String, valor: String?) {
        if (valor == null) campoNulo(chave) else campo(chave, valor)
    }

    fun campoInteiroOuNulo(chave: String, valor: Long?) {
        if (valor == null) campoNulo(chave) else campo(chave, valor)
    }

    fun campoInteiroOuNulo(chave: String, valor: Int?) {
        if (valor == null) campoNulo(chave) else campo(chave, valor.toLong())
    }

    fun campoTexto(chave: String, valores: List<String>) =
        campo(chave, JsonLista(valores.map { JsonTexto(it) }))

    fun campoInteiros(chave: String, valores: List<Long>) =
        campo(chave, JsonLista(valores.map { JsonInteiro(it) }))

    @JvmName("campoInteirosInt")
    fun campoInteiros(chave: String, valores: List<Int>) =
        campo(chave, JsonLista(valores.map { JsonInteiro(it.toLong()) }))

    fun construir(): JsonObjeto = JsonObjeto(campos)
}

fun jsonObjeto(bloco: JsonObjetoBuilder.() -> Unit): JsonObjeto =
    JsonObjetoBuilder().apply(bloco).construir()

object JsonEscritor {

    /** Escrita canônica: chaves ordenadas, sem espaços. */
    fun canonico(valor: JsonValor): String =
        StringBuilder().also { escreverCanonico(valor, it) }.toString()

    /** Escrita legível, com chaves ordenadas e indentação. */
    fun bonito(valor: JsonValor, indentacao: Int = 2): String =
        StringBuilder().also { escreverBonito(valor, it, 0, indentacao) }.toString()

    private fun escreverCanonico(valor: JsonValor, sb: StringBuilder) {
        when (valor) {
            is JsonObjeto -> {
                sb.append('{')
                var primeiro = true
                valor.campos.keys.sorted().forEach { chave ->
                    if (!primeiro) sb.append(',')
                    primeiro = false
                    escreverTexto(chave, sb)
                    sb.append(':')
                    escreverCanonico(valor.campos.getValue(chave), sb)
                }
                sb.append('}')
            }
            is JsonLista -> {
                sb.append('[')
                valor.itens.forEachIndexed { indice, item ->
                    if (indice > 0) sb.append(',')
                    escreverCanonico(item, sb)
                }
                sb.append(']')
            }
            is JsonTexto -> escreverTexto(valor.valor, sb)
            is JsonInteiro -> sb.append(valor.valor)
            is JsonBooleano -> sb.append(if (valor.valor) "true" else "false")
            JsonNulo -> sb.append("null")
        }
    }

    private fun escreverBonito(
        valor: JsonValor,
        sb: StringBuilder,
        nivel: Int,
        indentacao: Int,
    ) {
        when (valor) {
            is JsonObjeto -> {
                if (valor.campos.isEmpty()) {
                    sb.append("{}")
                    return
                }
                sb.append("{\n")
                val chaves = valor.campos.keys.sorted()
                chaves.forEachIndexed { indice, chave ->
                    sb.append(" ".repeat((nivel + 1) * indentacao))
                    escreverTexto(chave, sb)
                    sb.append(": ")
                    escreverBonito(valor.campos.getValue(chave), sb, nivel + 1, indentacao)
                    if (indice < chaves.size - 1) sb.append(',')
                    sb.append('\n')
                }
                sb.append(" ".repeat(nivel * indentacao)).append('}')
            }
            is JsonLista -> {
                if (valor.itens.isEmpty()) {
                    sb.append("[]")
                    return
                }
                sb.append("[\n")
                valor.itens.forEachIndexed { indice, item ->
                    sb.append(" ".repeat((nivel + 1) * indentacao))
                    escreverBonito(item, sb, nivel + 1, indentacao)
                    if (indice < valor.itens.size - 1) sb.append(',')
                    sb.append('\n')
                }
                sb.append(" ".repeat(nivel * indentacao)).append(']')
            }
            else -> escreverCanonico(valor, sb)
        }
    }

    private fun escreverTexto(texto: String, sb: StringBuilder) {
        sb.append('"')
        texto.forEach { caractere ->
            when (caractere) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else ->
                    if (caractere < ' ') {
                        sb.append("\\u").append(caractere.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(caractere)
                    }
            }
        }
        sb.append('"')
    }
}

object JsonLeitor {

    fun ler(texto: String, profundidadeMaxima: Int = PROFUNDIDADE_PADRAO): JsonValor {
        val parser = Parser(texto, profundidadeMaxima)
        val valor = parser.lerValor(0)
        parser.pularEspacos()
        if (!parser.fim()) {
            throw JsonInvalidoException("Conteúdo extra após o fim do JSON.")
        }
        return valor
    }

    const val PROFUNDIDADE_PADRAO = 64

    private class Parser(private val texto: String, private val profundidadeMaxima: Int) {
        private var posicao = 0

        fun fim(): Boolean = posicao >= texto.length

        fun pularEspacos() {
            while (posicao < texto.length && texto[posicao].isWhitespace()) posicao++
        }

        fun lerValor(profundidade: Int): JsonValor {
            if (profundidade > profundidadeMaxima) {
                throw JsonInvalidoException("Estrutura aninhada além do limite permitido.")
            }
            pularEspacos()
            if (fim()) throw JsonInvalidoException("JSON vazio ou incompleto.")
            return when (val caractere = texto[posicao]) {
                '{' -> lerObjeto(profundidade)
                '[' -> lerLista(profundidade)
                '"' -> JsonTexto(lerTexto())
                't' -> { consumirPalavra("true"); JsonBooleano(true) }
                'f' -> { consumirPalavra("false"); JsonBooleano(false) }
                'n' -> { consumirPalavra("null"); JsonNulo }
                '-', in '0'..'9' -> JsonInteiro(lerInteiro())
                else -> throw JsonInvalidoException("Caractere inesperado: '$caractere'.")
            }
        }

        private fun consumirPalavra(palavra: String) {
            if (posicao + palavra.length > texto.length ||
                texto.substring(posicao, posicao + palavra.length) != palavra
            ) {
                throw JsonInvalidoException("Token inválido.")
            }
            posicao += palavra.length
        }

        private fun lerObjeto(profundidade: Int): JsonObjeto {
            posicao++ // '{'
            val campos = linkedMapOf<String, JsonValor>()
            pularEspacos()
            if (posicao < texto.length && texto[posicao] == '}') {
                posicao++
                return JsonObjeto(campos)
            }
            while (true) {
                pularEspacos()
                if (fim() || texto[posicao] != '"') {
                    throw JsonInvalidoException("Chave de objeto inválida.")
                }
                val chave = lerTexto()
                if (campos.containsKey(chave)) {
                    throw JsonInvalidoException("Chave duplicada no objeto: \"$chave\".")
                }
                pularEspacos()
                if (fim() || texto[posicao] != ':') {
                    throw JsonInvalidoException("Esperado ':' após a chave.")
                }
                posicao++
                campos[chave] = lerValor(profundidade + 1)
                pularEspacos()
                if (fim()) throw JsonInvalidoException("Objeto não finalizado.")
                when (texto[posicao]) {
                    ',' -> posicao++
                    '}' -> {
                        posicao++
                        return JsonObjeto(campos)
                    }
                    else -> throw JsonInvalidoException("Esperado ',' ou '}' no objeto.")
                }
            }
        }

        private fun lerLista(profundidade: Int): JsonLista {
            posicao++ // '['
            val itens = mutableListOf<JsonValor>()
            pularEspacos()
            if (posicao < texto.length && texto[posicao] == ']') {
                posicao++
                return JsonLista(itens)
            }
            while (true) {
                itens.add(lerValor(profundidade + 1))
                pularEspacos()
                if (fim()) throw JsonInvalidoException("Lista não finalizada.")
                when (texto[posicao]) {
                    ',' -> posicao++
                    ']' -> {
                        posicao++
                        return JsonLista(itens)
                    }
                    else -> throw JsonInvalidoException("Esperado ',' ou ']' na lista.")
                }
            }
        }

        private fun lerTexto(): String {
            posicao++ // '"'
            val sb = StringBuilder()
            while (posicao < texto.length) {
                val caractere = texto[posicao++]
                when {
                    caractere == '"' -> return sb.toString()
                    caractere == '\\' -> {
                        if (fim()) throw JsonInvalidoException("Escape incompleto.")
                        when (val escape = texto[posicao++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> sb.append(lerUnicode())
                            else -> throw JsonInvalidoException("Escape inválido: \\$escape.")
                        }
                    }
                    caractere < ' ' -> throw JsonInvalidoException("Controle inválido em texto.")
                    else -> sb.append(caractere)
                }
            }
            throw JsonInvalidoException("Texto não finalizado.")
        }

        private fun lerUnicode(): Char {
            if (posicao + 4 > texto.length) throw JsonInvalidoException("\\u incompleto.")
            val hex = texto.substring(posicao, posicao + 4)
            val codigo = hex.toIntOrNull(16) ?: throw JsonInvalidoException("\\u inválido.")
            posicao += 4
            return codigo.toChar()
        }

        private fun lerInteiro(): Long {
            val inicio = posicao
            if (texto[posicao] == '-') posicao++
            if (fim() || texto[posicao] !in '0'..'9') {
                throw JsonInvalidoException("Número inválido.")
            }
            while (posicao < texto.length && texto[posicao] in '0'..'9') posicao++
            if (posicao < texto.length && (texto[posicao] == '.' ||
                    texto[posicao] == 'e' || texto[posicao] == 'E')
            ) {
                throw JsonInvalidoException("Apenas números inteiros são suportados.")
            }
            val bruto = texto.substring(inicio, posicao)
            return bruto.toLongOrNull()
                ?: throw JsonInvalidoException("Número fora do intervalo suportado.")
        }
    }
}
