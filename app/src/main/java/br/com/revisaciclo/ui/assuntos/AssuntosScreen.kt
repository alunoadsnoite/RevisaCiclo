package br.com.revisaciclo.ui.assuntos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.ui.common.SeletorData
import br.com.revisaciclo.ui.common.formatarData
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssuntosScreen(
    disciplinaId: Long,
    onVoltar: () -> Unit,
    viewModel: AssuntosViewModel = viewModel(factory = AssuntosViewModel.factory(disciplinaId)),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var editando by remember { mutableStateOf<Assunto?>(null) }
    var exibindoDialogo by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<Assunto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = estado.disciplina?.nome ?: "Assuntos") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editando = null
                    exibindoDialogo = true
                },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Novo assunto")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (!estado.carregando && estado.assuntos.isEmpty()) {
                Text(
                    text = "Nenhum assunto cadastrado ainda.\nToque em + para adicionar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = estado.assuntos, key = { it.id }) { assunto ->
                        ItemAssunto(
                            assunto = assunto,
                            onEditar = {
                                editando = assunto
                                exibindoDialogo = true
                            },
                            onExcluir = { excluindo = assunto },
                        )
                    }
                }
            }
        }
    }

    if (exibindoDialogo) {
        DialogoAssunto(
            assunto = editando,
            onDispensar = { exibindoDialogo = false },
            onSalvar = { titulo, descricao, dataEstudo ->
                viewModel.salvar(editando?.id, titulo, descricao, dataEstudo)
                exibindoDialogo = false
            },
        )
    }

    excluindo?.let { assunto ->
        AlertDialog(
            onDismissRequest = { excluindo = null },
            title = { Text(text = "Excluir assunto") },
            text = { Text(text = "Excluir \"${assunto.titulo}\" e suas revisões?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.excluir(assunto.id)
                        excluindo = null
                    },
                ) {
                    Text(text = "Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { excluindo = null }) {
                    Text(text = "Cancelar")
                }
            },
        )
    }
}

@Composable
private fun ItemAssunto(
    assunto: Assunto,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = assunto.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                EtiquetaEstado(assunto.estadoRevisao)
                Box {
                    IconButton(onClick = { menuAberto = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opções")
                    }
                    DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        DropdownMenuItem(
                            text = { Text(text = "Editar") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuAberto = false
                                onEditar()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "Excluir") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuAberto = false
                                onExcluir()
                            },
                        )
                    }
                }
            }

            assunto.descricao?.let { descricao ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Estudado em ${formatarData(assunto.dataEstudo)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            assunto.proximaRevisao?.let { proxima ->
                Text(
                    text = "Próxima revisão: ${formatarData(proxima)} (ciclo ${assunto.indiceCiclo})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EtiquetaEstado(estado: EstadoRevisao) {
    val (texto, cor) = when (estado) {
        EstadoRevisao.NOVO -> "Novo" to MaterialTheme.colorScheme.secondaryContainer
        EstadoRevisao.EM_DIA -> "Em dia" to MaterialTheme.colorScheme.primaryContainer
        EstadoRevisao.ATRASADO -> "Atrasado" to MaterialTheme.colorScheme.errorContainer
    }
    Surface(
        color = cor,
        shape = CircleShape,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun DialogoAssunto(
    assunto: Assunto?,
    onDispensar: () -> Unit,
    onSalvar: (String, String?, LocalDate) -> Unit,
) {
    var titulo by remember { mutableStateOf(assunto?.titulo.orEmpty()) }
    var descricao by remember { mutableStateOf(assunto?.descricao.orEmpty()) }
    var dataEstudo by remember { mutableStateOf(assunto?.dataEstudo ?: LocalDate.now()) }
    var selecionandoData by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = if (assunto == null) "Novo assunto" else "Editar assunto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text(text = "Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text(text = "Descrição (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    Text(
                        text = "Data de estudo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { selecionandoData = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = formatarData(dataEstudo))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(titulo, descricao.ifBlank { null }, dataEstudo) }) {
                Text(text = "Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDispensar) { Text(text = "Cancelar") }
        },
    )

    if (selecionandoData) {
        SeletorData(
            dataInicial = dataEstudo,
            onConfirmar = { dataEstudo = it },
            onDispensar = { selecionandoData = false },
        )
    }
}
