package br.com.revisaciclo.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_DATA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

fun formatarData(data: LocalDate): String = data.format(FORMATO_DATA)

val CORES_DISCIPLINA: List<Long> = listOf(
    0xFF1565C0L,
    0xFF2E7D32L,
    0xFFEF6C00L,
    0xFFC62828L,
    0xFF6A1B9AL,
    0xFF00838FL,
    0xFFAD1457L,
    0xFF4527A0L,
    0xFF9E9D24L,
    0xFF37474FL,
)
