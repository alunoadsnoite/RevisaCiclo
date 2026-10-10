package br.com.revisaciclo.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTest {

    @Test
    fun `escritor canonico ordena chaves`() {
        val obj = jsonObjeto {
            campo("b", 2)
            campo("a", 1)
        }
        assertEquals("{\"a\":1,\"b\":2}", JsonEscritor.canonico(obj))
    }

    @Test
    fun `escritor rejeita texto vazio`() {
        assertEquals("{\"a\":\"\"}", JsonEscritor.canonico(jsonObjeto { campo("a", "") }))
    }

    @Test
    fun `inteiro nunca é float`() {
        val texto = JsonEscritor.canonico(jsonObjeto { campo("x", 10) })
        assertEquals("{\"x\":10}", texto)
    }
}
