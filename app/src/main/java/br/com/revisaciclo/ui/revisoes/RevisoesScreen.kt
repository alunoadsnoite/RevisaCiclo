package br.com.revisaciclo.ui.revisoes

import br.com.revisaciclo.R
import br.com.revisaciclo.ui.common.recursosLocalizados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.ui.common.OpcoesAvaliacao
import br.com.revisaciclo.ui.common.formatarData
import br.com.revisaciclo.ui.projetos.SeletorProjetoAtivo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisoesScreen(
    onIniciarSessao: () -> Unit,
    onGerenciarProjetos: () -> Unit = {},
    viewModel: RevisoesViewModel = viewModel(factory = RevisoesViewModel.Factory),
) {
    val textos = recursosLocalizados()
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var avaliando by remember { mutableStateOf<RevisaoPendente?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = textos.getString(R.string.reviews)) },
                actions = { SeletorProjetoAtivo(onGerenciarProjetos = onGerenciarProjetos) },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (!estado.carregando && estado.pendentes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = textos.getString(R.string.no_pending_reviews_all_caught_up),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = onIniciarSessao) {
                        Text(text = textos.getString(R.string.start_session))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        FilledTonalButton(
                            onClick = onIniciarSessao,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = textos.getString(R.string.start_review_session))
                        }
                    }
                    if (estado.atrasadas > 0) {
                        item {
                            Text(
                                text = textos.getString(R.string.overdue_reviews_format, estado.atrasadas),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    items(items = estado.pendentes, key = { it.revisaoId }) { pendente ->
                        ItemRevisaoPendente(
                            pendente = pendente,
                            onAvaliar = { avaliando = pendente },
                        )
                    }
                }
            }
        }
    }

    avaliando?.let { pendente ->
        DialogoAvaliacao(
            pendente = pendente,
            onDispensar = { avaliando = null },
            onAvaliar = { avaliacao ->
                viewModel.avaliar(pendente.revisaoId, avaliacao)
                avaliando = null
            },
        )
    }
}

@Composable
private fun ItemRevisaoPendente(
    pendente: RevisaoPendente,
    onAvaliar: () -> Unit,
) {
    val textos = recursosLocalizados()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    color = Color(pendente.disciplinaCor.toInt()),
                    shape = CircleShape,
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pendente.assuntoTitulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (pendente.atrasada) {
                    Text(
                        text = textos.getString(R.string.overdue_3),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pendente.disciplinaNome,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = textos.getString(R.string.scheduled_for_cycle_format, formatarData(pendente.agendadaPara), pendente.indiceCiclo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onAvaliar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = textos.getString(R.string.rate_review))
            }
        }
    }
}

@Composable
private fun DialogoAvaliacao(
    pendente: RevisaoPendente,
    onDispensar: () -> Unit,
    onAvaliar: (AvaliacaoRevisao) -> Unit,
) {
    val textos = recursosLocalizados()
    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = textos.getString(R.string.how_did_the_review_go)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = pendente.assuntoTitulo,
                    style = MaterialTheme.typography.titleSmall,
                )
                OpcoesAvaliacao(onAvaliar = onAvaliar)
            }
        },
        confirmButton = {
            TextButton(onClick = onDispensar) { Text(text = textos.getString(R.string.cancel)) }
        },
    )
}
