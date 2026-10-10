package br.com.revisaciclo.data.repository

import br.com.revisaciclo.data.local.dao.AssuntoDao
import br.com.revisaciclo.data.local.dao.DisciplinaDao
import br.com.revisaciclo.data.local.dao.RevisaoDao
import br.com.revisaciclo.data.local.entity.AssuntoEntity
import br.com.revisaciclo.data.local.entity.DisciplinaEntity
import br.com.revisaciclo.data.local.entity.RevisaoEntity

object DemoDataSeeder {
    private const val DIA_MS = 86_400_000L

    suspend fun popular(
        disciplinaDao: DisciplinaDao,
        assuntoDao: AssuntoDao,
        revisaoDao: RevisaoDao,
        agora: Long = System.currentTimeMillis(),
    ) {
        val idsDisciplinas = disciplinaDao.inserir(
            listOf(
                DisciplinaEntity(nome = "Matemática", cor = 0xFF1565C0L, criadaEm = agora - 30 * DIA_MS),
                DisciplinaEntity(nome = "Biologia", cor = 0xFF2E7D32L, criadaEm = agora - 25 * DIA_MS),
                DisciplinaEntity(nome = "História", cor = 0xFFEF6C00L, criadaEm = agora - 20 * DIA_MS),
            ),
        )

        val assuntos = listOf(
            Triple(0, "Funções", 3),
            Triple(0, "Geometria", 2),
            Triple(0, "Probabilidade", 4),
            Triple(1, "Citologia", 2),
            Triple(1, "Genética", 4),
            Triple(2, "Brasil Colônia", 3),
            Triple(2, "Era Vargas", 2),
        )

        val idsAssuntos = assuntoDao.inserir(
            assuntos.map { (indiceDisciplina, nome, dificuldade) ->
                AssuntoEntity(
                    disciplinaId = idsDisciplinas[indiceDisciplina],
                    nome = nome,
                    dificuldade = dificuldade,
                    criadoEm = agora - 15 * DIA_MS,
                )
            },
        )

        val intervalos = listOf(1, 7, 30)
        val revisoes = buildList {
            idsAssuntos.forEachIndexed { indiceAssunto, assuntoId ->
                intervalos.forEachIndexed { indiceCiclo, intervalo ->
                    val revisao = when (indiceCiclo) {
                        0 -> RevisaoEntity(
                            assuntoId = assuntoId,
                            agendadaPara = agora - 5 * DIA_MS,
                            concluidaEm = agora - 5 * DIA_MS,
                            status = "CONCLUIDA",
                            intervaloDias = intervalo,
                            indiceCiclo = indiceCiclo + 1,
                            qualidade = 4,
                        )

                        1 -> if (indiceAssunto % 2 == 0) {
                            RevisaoEntity(
                                assuntoId = assuntoId,
                                agendadaPara = agora - DIA_MS,
                                concluidaEm = agora - DIA_MS,
                                status = "CONCLUIDA",
                                intervaloDias = intervalo,
                                indiceCiclo = indiceCiclo + 1,
                                qualidade = 3,
                            )
                        } else {
                            RevisaoEntity(
                                assuntoId = assuntoId,
                                agendadaPara = agora - DIA_MS,
                                status = "ATRASADA",
                                intervaloDias = intervalo,
                                indiceCiclo = indiceCiclo + 1,
                            )
                        }

                        else -> RevisaoEntity(
                            assuntoId = assuntoId,
                            agendadaPara = agora + 4 * DIA_MS,
                            status = "PENDENTE",
                            intervaloDias = intervalo,
                            indiceCiclo = indiceCiclo + 1,
                        )
                    }
                    add(revisao)
                }
            }
        }

        revisaoDao.inserir(revisoes)
    }
}
