package br.com.revisaciclo.domain.backup

import java.security.MessageDigest

/**
 * Cálculo de integridade do payload de backup.
 *
 * O checksum é o SHA-256, em hexadecimal minúsculo, dos bytes UTF-8 da
 * serialização **canônica** do payload (chaves ordenadas, sem espaços).
 */
object Checksum {

    const val ALGORITMO = "SHA-256"

    fun sha256Hex(conteudo: ByteArray): String {
        val digest = MessageDigest.getInstance(ALGORITMO).digest(conteudo)
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun sha256Hex(texto: String): String = sha256Hex(texto.toByteArray(Charsets.UTF_8))
}
