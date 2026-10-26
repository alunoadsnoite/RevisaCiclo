package br.com.revisaciclo.ui.common

import br.com.revisaciclo.R
import br.com.revisaciclo.ui.common.recursosLocalizados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.revisaciclo.domain.model.AvaliacaoRevisao

@Composable
fun OpcoesAvaliacao(
    habilitado: Boolean = true,
    onAvaliar: (AvaliacaoRevisao) -> Unit,
) {
    val textos = recursosLocalizados()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Avaliacao(rotulo = textos.getString(R.string.forgot), descricao = textos.getString(R.string.restart_with_a_1_day_interval), habilitado) {
            onAvaliar(AvaliacaoRevisao.ESQUECI)
        }
        Avaliacao(rotulo = textos.getString(R.string.hard), descricao = textos.getString(R.string.repeat_the_current_cycle), habilitado) {
            onAvaliar(AvaliacaoRevisao.DIFICIL)
        }
        Avaliacao(rotulo = textos.getString(R.string.good), descricao = textos.getString(R.string.advance_one_cycle), habilitado) {
            onAvaliar(AvaliacaoRevisao.BOM)
        }
        Avaliacao(rotulo = textos.getString(R.string.easy), descricao = textos.getString(R.string.advance_two_cycles), habilitado) {
            onAvaliar(AvaliacaoRevisao.FACIL)
        }
    }
}

@Composable
private fun Avaliacao(
    rotulo: String,
    descricao: String,
    habilitado: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(text = rotulo, fontWeight = FontWeight.SemiBold)
                Text(
                    text = descricao,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}