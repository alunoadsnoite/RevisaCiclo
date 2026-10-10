package br.com.revisaciclo.ui.common

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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Avaliacao(rotulo = "Esqueci", descricao = "Reinicia o ciclo para 1 dia", habilitado) {
            onAvaliar(AvaliacaoRevisao.ESQUECI)
        }
        Avaliacao(rotulo = "Difícil", descricao = "Repete o ciclo atual", habilitado) {
            onAvaliar(AvaliacaoRevisao.DIFICIL)
        }
        Avaliacao(rotulo = "Bom", descricao = "Avança um ciclo", habilitado) {
            onAvaliar(AvaliacaoRevisao.BOM)
        }
        Avaliacao(rotulo = "Fácil", descricao = "Avança dois ciclos", habilitado) {
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