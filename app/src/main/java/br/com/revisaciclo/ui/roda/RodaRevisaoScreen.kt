@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.revisaciclo.ui.roda

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.revisaciclo.domain.model.AssuntoRoda
import br.com.revisaciclo.domain.model.EstagioRoda
import br.com.revisaciclo.domain.model.SituacaoRevisao
import br.com.revisaciclo.ui.common.formatarData

@Composable
fun RodaRevisaoScreen(
    onIniciarSessao: () -> Unit,
    viewModel: RodaRevisaoViewModel = viewModel(factory = RodaRevisaoViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.atualizarData()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "Estágios") }) },
    ) { padding ->
        val roda = estado.roda
        if (estado.carregando || roda == null) {
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
                RodaCircular(
                    totalAssuntos = roda.totalAssuntos,
                    estagios = roda.estagios,
                    estagioSelecionado = estado.estagioSelecionado,
                    onSelecionarEstagio = viewModel::selecionarEstagio,
                )

                if (roda.estaVazia) {
                    Text(
                        text = "Cadastre disciplinas e assuntos para começar a revisar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    FilledTonalButton(
                        onClick = onIniciarSessao,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Iniciar sessão de revisão")
                    }
                }

                Text(
                    text = "Estágios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                roda.estagios.forEach { estagio ->
                    LinhaEstagio(
                        estagio = estagio,
                        selecionado = estado.estagioSelecionado == estagio.intervaloDias,
                        onSelecionar = { viewModel.selecionarEstagio(estagio.intervaloDias) },
                    )
                }

                estado.estagioSelecionado?.let { selecionado ->
                    val estagio = roda.estagios.firstOrNull { it.intervaloDias == selecionado }
                    if (estagio != null) {
                        DetalheEstagio(estagio = estagio)
                    }
                }
            }
        }
    }
}

@Composable
private fun RodaCircular(
    totalAssuntos: Int,
    estagios: List<EstagioRoda>,
    estagioSelecionado: Int?,
    onSelecionarEstagio: (Int) -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val descricaoAcessivel = remember(totalAssuntos, estagios) {
        buildString {
            append("Roda de revisão com $totalAssuntos assuntos. ")
            estagios.forEach { estagio ->
                val rotulo = if (estagio.intervaloDias == 1) "1 dia" else "${estagio.intervaloDias} dias"
                append("$rotulo: ${estagio.total} assuntos, ${estagio.atrasados} atrasados. ")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = descricaoAcessivel }
                .pointerInput(estagios, totalAssuntos) {
                    detectTapGestures { offset ->
                        val alvo = Size(size.width.toFloat(), size.height.toFloat())
                        estagioNoPonto(offset, alvo, estagios, totalAssuntos)
                            ?.let(onSelecionarEstagio)
                    }
                },
        ) {
            val anel = calcularAnel(size)
            val estilo = Stroke(width = anel.traco, cap = StrokeCap.Butt)

            if (totalAssuntos == 0) {
                drawArc(
                    color = esquema.surfaceVariant,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = anel.topLeft,
                    size = anel.tamanho,
                    style = estilo,
                )
                return@Canvas
            }

            var inicio = -90f
            estagios.forEach { estagio ->
                val varredura = 360f * estagio.total / totalAssuntos
                if (varredura > 0f) {
                    drawArc(
                        color = corEstagio(estagio, esquema),
                        startAngle = inicio,
                        sweepAngle = varredura,
                        useCenter = false,
                        topLeft = anel.topLeft,
                        size = anel.tamanho,
                        style = estilo,
                    )
                }
                inicio += varredura
            }

            val selecionado = estagios.firstOrNull { it.intervaloDias == estagioSelecionado }
            if (selecionado != null && selecionado.total > 0) {
                val inicioSelecionado = estagios
                    .takeWhile { it.intervaloDias != selecionado.intervaloDias }
                    .sumOf { it.total }
                val varredura = 360f * selecionado.total / totalAssuntos
                drawArc(
                    color = esquema.onSurface,
                    startAngle = -90f + 360f * inicioSelecionado / totalAssuntos,
                    sweepAngle = varredura,
                    useCenter = false,
                    topLeft = anel.topLeft,
                    size = anel.tamanho,
                    style = estilo,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$totalAssuntos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "assuntos",
                style = MaterialTheme.typography.labelMedium,
                color = esquema.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinhaEstagio(
    estagio: EstagioRoda,
    selecionado: Boolean,
    onSelecionar: () -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val rotulo = if (estagio.intervaloDias == 1) "1 dia" else "${estagio.intervaloDias} dias"
    Card(
        onClick = onSelecionar,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(14.dp),
                color = corEstagio(estagio, esquema),
                shape = CircleShape,
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rotulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${estagio.total} assuntos · ${estagio.atrasados} atrasados · ${estagio.hoje} vencem hoje",
                    style = MaterialTheme.typography.bodySmall,
                    color = esquema.onSurfaceVariant,
                )
            }
            RadioButton(selected = selecionado, onClick = onSelecionar)
        }
    }
}

@Composable
private fun DetalheEstagio(estagio: EstagioRoda) {
    val esquema = MaterialTheme.colorScheme
    val rotulo = if (estagio.intervaloDias == 1) "1 dia" else "${estagio.intervaloDias} dias"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Assuntos em $rotulo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (estagio.assuntos.isEmpty()) {
                Text(
                    text = "Nenhum assunto neste estágio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = esquema.onSurfaceVariant,
                )
            } else {
                estagio.assuntos.forEach { assunto ->
                    ListaAssuntoRoda(assunto = assunto, esquema = esquema)
                }
            }
        }
    }
}

@Composable
private fun ListaAssuntoRoda(assunto: AssuntoRoda, esquema: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            color = Color(assunto.disciplinaCor.toInt()),
            shape = CircleShape,
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = assunto.titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = assunto.disciplinaNome,
                style = MaterialTheme.typography.bodySmall,
                color = esquema.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        SituacaoRevisao(assunto = assunto)
    }
}

@Composable
private fun SituacaoRevisao(assunto: AssuntoRoda) {
    val esquema = MaterialTheme.colorScheme
    val (rotulo, container, conteudo) = when (assunto.situacao) {
        SituacaoRevisao.ATRASADO -> Triple("Atrasada", esquema.errorContainer, esquema.onErrorContainer)
        SituacaoRevisao.HOJE -> Triple("Vence hoje", esquema.tertiaryContainer, esquema.onTertiaryContainer)
        SituacaoRevisao.FUTURO -> {
            val texto = assunto.proximaRevisao
                ?.let { "Vence ${formatarData(it)}" }
                ?: "Inicial"
            Triple(texto, esquema.secondaryContainer, esquema.onSecondaryContainer)
        }
    }
    Surface(
        color = container,
        contentColor = conteudo,
        shape = CircleShape,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private fun corEstagio(estagio: EstagioRoda, esquema: ColorScheme): Color = when {
    estagio.atrasados > 0 -> esquema.error
    estagio.hoje > 0 -> esquema.tertiary
    else -> esquema.primary
}