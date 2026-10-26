package br.com.revisaciclo.ui.disciplinas

import br.com.revisaciclo.R
import br.com.revisaciclo.ui.common.recursosLocalizados

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.ui.common.CORES_DISCIPLINA
import br.com.revisaciclo.ui.projetos.SeletorProjetoAtivo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplinasScreen(
    onAbrirAssuntos: (Long) -> Unit,
    onGerenciarProjetos: () -> Unit = {},
    viewModel: DisciplinasViewModel = viewModel(factory = DisciplinasViewModel.Factory),
) {
    val textos = recursosLocalizados()
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var editando by remember { mutableStateOf<Disciplina?>(null) }
    var exibindoDialogo by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<Disciplina?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = textos.getString(R.string.subjects)) },
                actions = { SeletorProjetoAtivo(onGerenciarProjetos = onGerenciarProjetos) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editando = null
                    exibindoDialogo = true
                },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = textos.getString(R.string.new_subject))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (!estado.carregando && estado.disciplinas.isEmpty()) {
                Text(
                    text = textos.getString(R.string.no_subjects_yet_tap_to_create_your_first_one),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = estado.disciplinas, key = { it.id }) { disciplina ->
                        ItemDisciplina(
                            disciplina = disciplina,
                            onAbrir = { onAbrirAssuntos(disciplina.id) },
                            onEditar = {
                                editando = disciplina
                                exibindoDialogo = true
                            },
                            onExcluir = { excluindo = disciplina },
                        )
                    }
                }
            }
        }
    }

    if (exibindoDialogo) {
        DialogoDisciplina(
            disciplina = editando,
            onDispensar = { exibindoDialogo = false },
            onSalvar = { nome, cor ->
                viewModel.salvar(editando?.id, nome, cor)
                exibindoDialogo = false
            },
        )
    }

    excluindo?.let { disciplina ->
        AlertDialog(
            onDismissRequest = { excluindo = null },
            title = { Text(text = textos.getString(R.string.delete_subject)) },
            text = {
                Text(
                    text = textos.getString(R.string.delete_and_all_related_topics_and_reviews_format, disciplina.nome),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.excluir(disciplina.id)
                        excluindo = null
                    },
                ) {
                    Text(text = textos.getString(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { excluindo = null }) {
                    Text(text = textos.getString(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDisciplina(
    disciplina: Disciplina,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
) {
    val textos = recursosLocalizados()
    var menuAberto by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(16.dp),
                color = Color(disciplina.cor.toInt()),
                shape = CircleShape,
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = disciplina.nome,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menuAberto = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = textos.getString(R.string.options))
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    DropdownMenuItem(
                        text = { Text(text = textos.getString(R.string.edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuAberto = false
                            onEditar()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = textos.getString(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuAberto = false
                            onExcluir()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoDisciplina(
    disciplina: Disciplina?,
    onDispensar: () -> Unit,
    onSalvar: (String, Long) -> Unit,
) {
    val textos = recursosLocalizados()
    var nome by remember { mutableStateOf(disciplina?.nome.orEmpty()) }
    var cor by remember { mutableLongStateOf(disciplina?.cor ?: CORES_DISCIPLINA.first()) }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = if (disciplina == null) textos.getString(R.string.new_subject) else textos.getString(R.string.edit_subject)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text(text = textos.getString(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = textos.getString(R.string.color),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                SeletorCor(selecionada = cor, onSelecionar = { cor = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(nome, cor) }) { Text(text = textos.getString(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDispensar) { Text(text = textos.getString(R.string.cancel)) }
        },
    )
}

@Composable
private fun SeletorCor(
    selecionada: Long,
    onSelecionar: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CORES_DISCIPLINA.forEach { cor ->
            val selecionado = cor == selecionada
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = Color(cor.toInt()), shape = CircleShape)
                    .border(
                        width = if (selecionado) 3.dp else 0.dp,
                        color = if (selecionado) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clickable { onSelecionar(cor) },
            )
        }
    }
}
