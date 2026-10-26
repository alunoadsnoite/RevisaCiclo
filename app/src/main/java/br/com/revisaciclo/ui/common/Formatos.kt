package br.com.revisaciclo.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatarData(data: LocalDate, locale: Locale = Locale.getDefault()): String {
    val pattern = if (locale.language == "en" && locale.country == "US") "MM/dd/yyyy" else "dd/MM/yyyy"
    return data.format(DateTimeFormatter.ofPattern(pattern, locale))
}

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
