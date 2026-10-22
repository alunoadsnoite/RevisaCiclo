@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.revisaciclo.ui.sessao

import br.com.revisaciclo.ui.common.localizarMensagem
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.ui.common.OpcoesAvaliacao
import br.com.revisaciclo.ui.common.formatarData
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun SessaoRevisaoScreen(
    onSair: () -> Unit,
    viewModel: SessaoRevisaoViewModel =
        viewModel(factory = SessaoRevisaoViewModel.factory()),
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
                title = { Text(text = textos.getString(R.string.review_session)) },
                navigationIcon = {
                    IconButton(onClick = onSair) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = textos.getString(R.string.exit))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val sessao = estado.sessao
            when {
                estado.carregando || sessao == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                sessao.total == 0 -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = textos.getString(R.string.no_reviews_due_right_now),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalVoltar(onSair)
                    }
                }
                sessao.concluida -> {
                    ResumoSessao(sessao = sessao, onFinalizar = onSair)
                }
                else -> {
                    estado.atual?.let { atual ->
                        ItemAtual(
                            estado = estado,
                            atual = atual,
                            onAvaliar = viewModel::avaliar,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilledTonalVoltar(onSair: () -> Unit) {
    val textos = recursosLocalizados()
    TextButton(onClick = onSair) {
        Text(text = textos.getString(R.string.back))
    }
}

@Composable
private fun ItemAtual(
    estado: SessaoRevisaoUiState,
    atual: RevisaoPendente,
    onAvaliar: (AvaliacaoRevisao) -> Unit,
) {
    val textos = recursosLocalizados()
    val sessao = estado.sessao
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = textos.getString(R.string.review_of_format, sessao?.concluidas?.plus(1) ?: 1, sessao?.total ?: 0),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        color = Color(atual.disciplinaCor.toInt()),
                        shape = CircleShape,
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = atual.assuntoTitulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
                atual.assuntoDescricao?.takeIf { it.isNotBlank() }?.let { descricao ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = descricao,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = textos.getString(R.string.cycle_due_format, atual.disciplinaNome, atual.indiceCiclo, formatarData(atual.agendadaPara)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (estado.erro != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = textos.localizarMensagem(estado.erro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        OpcoesAvaliacao(
            habilitado = !estado.processando,
            onAvaliar = onAvaliar,
        )
        if (estado.processando) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = textos.getString(R.string.saving_rating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResumoSessao(
    sessao: br.com.revisaciclo.domain.sessao.SessaoDeRevisao,
    onFinalizar: () -> Unit,
) {
    val textos = recursosLocalizados()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = textos.getString(R.string.session_complete),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = textos.getString(R.string.of_reviews_rated_format, sessao.concluidas, sessao.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    sessao.contagemResultados.forEach { (avaliacao, quantidade) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rotulo(avaliacao, textos),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "$quantidade",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = onFinalizar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = textos.getString(R.string.finish))
            }
        }
    }
}

private fun rotulo(avaliacao: AvaliacaoRevisao, textos: android.content.res.Resources): String = when (avaliacao) {
    AvaliacaoRevisao.ESQUECI -> textos.getString(R.string.forgot)
    AvaliacaoRevisao.DIFICIL -> textos.getString(R.string.hard)
    AvaliacaoRevisao.BOM -> textos.getString(R.string.good)
    AvaliacaoRevisao.FACIL -> textos.getString(R.string.easy)
}