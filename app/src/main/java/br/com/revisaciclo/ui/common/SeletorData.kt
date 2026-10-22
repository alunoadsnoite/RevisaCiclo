package br.com.revisaciclo.ui.common

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeletorData(
    dataInicial: LocalDate,
    onConfirmar: (LocalDate) -> Unit,
    onDispensar: () -> Unit,
) {
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = dataInicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDispensar,
        confirmButton = {
            TextButton(
                onClick = {
                    estado.selectedDateMillis?.let { millis ->
                        onConfirmar(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                    onDispensar()
                },
            ) {
                Text(text = getString(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDispensar) {
                Text(text = getString(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = estado)
    }
}
