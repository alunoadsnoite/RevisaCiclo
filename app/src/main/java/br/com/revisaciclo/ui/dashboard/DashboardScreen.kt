package br.com.revisaciclo.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.ProgressoMeta
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import br.com.revisaciclo.ui.common.formatarData
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onAbrirConfiguracoes: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.atualizarData()
        onPauseOrDispose { }
    }
    DashboardContent(estado = estado, onAbrirConfiguracoes = onAbrirConfiguracoes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    estado: DashboardUiState,
    onAbrirConfiguracoes: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Dashboard") },
                actions = {
                    IconButton(onClick = onAbrirConfiguracoes) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CartaoProgressoGeral(estado)

            CartaoMetaDiaria(estado.progressoMeta)

            LinhaIndicadores(estado)

            CartaoProximaRevisao(estado.proximaRevisao)

            Text(
                text = "Progresso por disciplina",
                style = MaterialTheme.typography.titleMedium,
            )

            if (estado.progressos.isEmpty()) {
                Text(
                    text = "Nenhuma disciplina cadastrada ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                estado.progressos.forEach { progresso ->
                    CartaoDisciplina(progresso)
                }
            }
        }
    }
}

@Composable
private fun CartaoProgressoGeral(estado: DashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProgressoCircular(progresso = estado.progressoGeral, tamanho = 180.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${estado.revisoesConcluidas} de ${estado.totalRevisoes} revisões concluídas",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${estado.revisoesPendentes} revisões pendentes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressoCircular(
    progresso: Float,
    modifier: Modifier = Modifier,
    tamanho: Dp = 160.dp,
) {
    val corPrincipal = MaterialTheme.colorScheme.primary
    val corTrilha = MaterialTheme.colorScheme.surfaceVariant
    val larguraTraco = 18.dp
    val progressoLimitado = progresso.coerceIn(0f, 1f)

    Box(
        modifier = modifier.size(tamanho),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val traco = larguraTraco.toPx()
            val diametro = size.minDimension - traco
            val canto = Offset((size.width - diametro) / 2f, (size.height - diametro) / 2f)
            val dimensao = Size(diametro, diametro)

            drawArc(
                color = corTrilha,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = canto,
                size = dimensao,
                style = Stroke(width = traco, cap = StrokeCap.Round),
            )
            drawArc(
                color = corPrincipal,
                startAngle = -90f,
                sweepAngle = 360f * progressoLimitado,
                useCenter = false,
                topLeft = canto,
                size = dimensao,
                style = Stroke(width = traco, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progressoLimitado * 100).roundToInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "concluído",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinhaIndicadores(estado: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Indicador(
            titulo = "Disciplinas",
            valor = estado.totalDisciplinas.toString(),
            modifier = Modifier.weight(1f),
        )
        Indicador(
            titulo = "Assuntos",
            valor = estado.totalAssuntos.toString(),
            modifier = Modifier.weight(1f),
        )
        Indicador(
            titulo = "Pendentes",
            valor = estado.revisoesPendentes.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Indicador(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoMetaDiaria(progresso: ProgressoMeta) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Meta diária",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${progresso.concluidasHoje} de ${progresso.metaDiaria} revisões hoje",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progresso.percentual },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (progresso.atingida) {
                    "Meta atingida. Bom trabalho!"
                } else {
                    "Faltam ${progresso.restantes} revisão(ões) para a meta."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoProximaRevisao(revisao: Revisao?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Próxima revisão",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (revisao == null) {
                Text(
                    text = "Nenhuma revisão agendada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = formatarData(revisao.agendadaPara),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (revisao.status == StatusRevisao.ATRASADA) {
                        "Atrasada — revisão do ciclo ${revisao.indiceCiclo}"
                    } else {
                        "Ciclo ${revisao.indiceCiclo} · intervalo de ${revisao.intervaloDias} dias"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CartaoDisciplina(progresso: ProgressoDisciplina) {
    val cor = Color(progresso.cor.toInt())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    color = cor,
                    shape = CircleShape,
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = progresso.nome,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${progresso.assuntosAtrasados} atrasado(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progresso.progresso },
                modifier = Modifier.fillMaxWidth(),
                color = cor,
            )
        }
    }
}
