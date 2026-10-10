package br.com.revisaciclo.domain.projetos

import br.com.revisaciclo.domain.model.ProjetoEstudo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeletorProjetoTest {

    private fun projeto(
        id: Long,
        criadoEm: Long,
        arquivado: Boolean = false,
    ) = ProjetoEstudo(
        id = id,
        nome = "Projeto $id",
        descricao = null,
        cor = 0xFF1565C0,
        criadoEm = criadoEm,
        arquivado = arquivado,
    )

    @Test
    fun `mantem o projeto selecionado quando valido`() {
        val projetos = listOf(projeto(1, criadoEm = 1), projeto(2, criadoEm = 2))

        val ativo = SeletorProjeto.resolver(projetos, selecionadoId = 2)

        assertEquals(2L, ativo?.id)
    }

    @Test
    fun `cai no projeto ativo mais antigo quando nenhum foi selecionado`() {
        val projetos = listOf(projeto(3, criadoEm = 30), projeto(1, criadoEm = 10), projeto(2, criadoEm = 20))

        val ativo = SeletorProjeto.resolver(projetos, selecionadoId = null)

        assertEquals(1L, ativo?.id)
    }

    @Test
    fun `ignora selecao inexistente`() {
        val projetos = listOf(projeto(1, criadoEm = 1), projeto(2, criadoEm = 2))

        val ativo = SeletorProjeto.resolver(projetos, selecionadoId = 99)

        assertEquals(1L, ativo?.id)
    }

    @Test
    fun `cai no proximo ativo quando o selecionado esta arquivado`() {
        val projetos = listOf(
            projeto(1, criadoEm = 1, arquivado = true),
            projeto(2, criadoEm = 2),
            projeto(3, criadoEm = 3),
        )

        val ativo = SeletorProjeto.resolver(projetos, selecionadoId = 1)

        assertEquals(2L, ativo?.id)
    }

    @Test
    fun `retorna nulo quando nenhum projeto esta ativo`() {
        val projetos = listOf(projeto(1, criadoEm = 1, arquivado = true))

        assertNull(SeletorProjeto.resolver(projetos, selecionadoId = 1))
        assertNull(SeletorProjeto.resolver(emptyList(), selecionadoId = null))
    }
}
