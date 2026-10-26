@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.revisaciclo.ui.estatisticas

import br.com.revisaciclo.R
import br.com.revisaciclo.ui.common.recursosLocalizados

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.revisaciclo.ui.projetos.SeletorProjetoAtivo
import br.com.revisaciclo.domain.model.ContagemDiaria
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EstatisticasScreen(
    onGerenciarProjetos: () -> Unit = {},
    viewModel: EstatisticasViewModel = viewModel(factory = EstatisticasViewModel.Factory),
) {
    val textos = recursosLocalizados()
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.atualizarData()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = textos.getString(R.string.statistics)) },
                actions = { SeletorProjetoAtivo(onGerenciarProjetos = onGerenciarProjetos) },
            )
        },
    ) { padding ->
        val estatisticas = estado.estatisticas
        val globais = estado.estatisticasGlobais
        if (estado.carregando || estatisticas == null || globais == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Secao(
                    titulo = textos.getString(R.string.active_project),
                    estatisticas = estatisticas,
                    comRotulos = true,
                )
                Secao(
                    titulo = textos.getString(R.string.global_totals_all_projects),
                    estatisticas = globais,
                    comRotulos = false,
                )
            }
        }
    }
}

@Composable
private fun Secao(
    titulo: String,
    estatisticas: br.com.revisaciclo.domain.model.EstatisticaRevisao,
    comRotulos: Boolean,
) {
    val textos = recursosLocalizados()
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    CartaoResumo(estatisticas = estatisticas)
    CartaoEvolucao(
        titulo = textos.getString(R.string.last_7_days),
        contagens = estatisticas.evolucao7Dias,
        comRotulos = comRotulos,
    )
    CartaoEvolucao(
        titulo = textos.getString(R.string.last_30_days),
        contagens = estatisticas.evolucao30Dias,
        comRotulos = false,
    )
}

@Composable
private fun CartaoResumo(estatisticas: br.com.revisaciclo.domain.model.EstatisticaRevisao) {
    val textos = recursosLocalizados()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Indicador(
                valor = estatisticas.concluidas,
                rotulo = textos.getString(R.string.completed),
                cor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Indicador(
                valor = estatisticas.pendentes,
                rotulo = textos.getString(R.string.pending),
                cor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
            Indicador(
                valor = estatisticas.atrasadas,
                rotulo = textos.getString(R.string.overdue_2),
                cor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = textos.getString(R.string.total_reviews_recorded_format, estatisticas.totalRevisoes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Indicador(
    valor: Int,
    rotulo: String,
    cor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "$valor",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = cor,
            )
            Text(
                text = rotulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoEvolucao(
    titulo: String,
    contagens: List<ContagemDiaria>,
    comRotulos: Boolean,
) {
    val textos = recursosLocalizados()
    val esquema = MaterialTheme.colorScheme
    val descricaoAcessivel = remember(contagens, textos) {
        buildString {
            append(textos.getString(R.string.daily_completed_reviews)).append(' ')
            contagens.forEachIndexed { indice, conta ->
                append("${br.com.revisaciclo.ui.common.formatarData(conta.data)}: ${conta.quantidade}")
                if (indice < contagens.lastIndex) append(". ")
            }
            append(".")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .semantics { contentDescription = descricaoAcessivel },
            ) {
                if (contagens.isEmpty()) return@Canvas
                val maximo = contagens.maxOf { it.quantidade }.coerceAtLeast(1)
                val larguraBarra = size.width / contagens.size
                val margem = larguraBarra * 0.15f
                val base = size.height - 4f
                contagens.forEachIndexed { indice, conta ->
                    val altura = (size.height - 4f) * conta.quantidade / maximo
                    drawRect(
                        color = if (conta.quantidade == 0) esquema.surfaceVariant else esquema.primary,
                        topLeft = Offset(indice * larguraBarra + margem, base - altura),
                        size = Size(larguraBarra - margem * 2f, altura),
                    )
                }
            }
            if (comRotulos) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    contagens.forEach { conta ->
                        Text(
                            text = rotuloDia(conta),
                            style = MaterialTheme.typography.labelSmall,
                            color = esquema.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun rotuloDia(conta: ContagemDiaria): String {
    val portugues = Locale.getDefault()
    val nome = conta.data.dayOfWeek.getDisplayName(TextStyle.SHORT, portugues)
    return nome.lowercase(portugues)
}