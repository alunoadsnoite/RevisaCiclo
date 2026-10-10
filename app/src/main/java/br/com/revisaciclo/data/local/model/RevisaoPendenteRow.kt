package br.com.revisaciclo.data.local.model

data class RevisaoPendenteRow(
    val revisaoId: Long,
    val assuntoId: Long,
    val assuntoTitulo: String,
    val disciplinaId: Long,
    val disciplinaNome: String,
    val disciplinaCor: Long,
    val agendadaPara: Long,
    val indiceCiclo: Int,
    val intervaloDias: Int,
)
