package br.com.revisaciclo.ui.projetos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.ui.common.CORES_DISCIPLINA

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjetosScreen(
    onVoltar: () -> Unit,
    viewModel: ProjetosViewModel = viewModel(factory = ProjetosViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var editando by remember { mutableStateOf<ProjetoEstudo?>(null) }
    var exibindoDialogo by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<ProjetoEstudo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Projetos") },
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
                Icon(imageVector = Icons.Default.Add, contentDescription = "Novo projeto")
            }
        },
    ) { padding ->
        if (!estado.carregando && estado.ativos.isEmpty() && estado.arquivados.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                Text(
                    text = "Nenhum projeto cadastrado ainda.\nToque em + para criar o primeiro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (estado.ativos.isNotEmpty()) {
                    item {
                        Text(
                            text = "Ativos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(items = estado.ativos, key = { it.id }) { projeto ->
                        ItemProjeto(
                            projeto = projeto,
                            ativo = projeto.id == estado.projetoAtivo?.id,
                            quantidadeDisciplinas = estado.contagem(projeto.id),
                            onSelecionar = { viewModel.selecionar(projeto.id) },
                            onEditar = {
                                editando = projeto
                                exibindoDialogo = true
                            },
                            onArquivar = { viewModel.arquivar(projeto.id) },
                            onExcluir = { excluindo = projeto },
                        )
                    }
                }

                if (estado.arquivados.isNotEmpty()) {
                    item {
                        Text(
                            text = "Arquivados",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(items = estado.arquivados, key = { it.id }) { projeto ->
                        ItemProjeto(
                            projeto = projeto,
                            ativo = false,
                            quantidadeDisciplinas = estado.contagem(projeto.id),
                            onSelecionar = { viewModel.desarquivar(projeto.id) },
                            onEditar = {
                                editando = projeto
                                exibindoDialogo = true
                            },
                            onArquivar = { viewModel.desarquivar(projeto.id) },
                            onExcluir = { excluindo = projeto },
                        )
                    }
                }
            }
        }
    }

    if (exibindoDialogo) {
        DialogoProjeto(
            projeto = editando,
            onDispensar = { exibindoDialogo = false },
            onSalvar = { nome, descricao, cor ->
                viewModel.salvar(editando?.id, nome, descricao, cor)
                exibindoDialogo = false
            },
        )
    }

    excluindo?.let { projeto ->
        AlertDialog(
            onDismissRequest = { excluindo = null },
            title = { Text(text = "Excluir projeto") },
            text = {
                Text(
                    text = "Excluir \"${projeto.nome}\" e todas as disciplinas, assuntos e " +
                        "revisões relacionadas? Esta ação não pode ser desfeita.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.excluir(projeto.id)
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
private fun ItemProjeto(
    projeto: ProjetoEstudo,
    ativo: Boolean,
    quantidadeDisciplinas: Int,
    onSelecionar: () -> Unit,
    onEditar: () -> Unit,
    onArquivar: () -> Unit,
    onExcluir: () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelecionar),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(16.dp),
                color = Color(projeto.cor.toInt()),
                shape = CircleShape,
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = projeto.nome,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (ativo) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Projeto ativo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                projeto.descricao?.let { descricao ->
                    Text(
                        text = descricao,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$quantidadeDisciplinas disciplina(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuAberto = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opções")
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    if (!projeto.arquivado) {
                        DropdownMenuItem(
                            text = { Text(text = "Definir como ativo") },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                            onClick = {
                                menuAberto = false
                                onSelecionar()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(text = "Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuAberto = false
                            onEditar()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(text = if (projeto.arquivado) "Desarquivar" else "Arquivar")
                        },
                        onClick = {
                            menuAberto = false
                            onArquivar()
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
    }
}

@Composable
private fun DialogoProjeto(
    projeto: ProjetoEstudo?,
    onDispensar: () -> Unit,
    onSalvar: (String, String?, Long) -> Unit,
) {
    var nome by remember { mutableStateOf(projeto?.nome.orEmpty()) }
    var descricao by remember { mutableStateOf(projeto?.descricao.orEmpty()) }
    var cor by remember { mutableLongStateOf(projeto?.cor ?: CORES_DISCIPLINA.first()) }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = if (projeto == null) "Novo projeto" else "Editar projeto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text(text = "Nome") },
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
                Text(
                    text = "Cor",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                SeletorCor(selecionada = cor, onSelecionar = { cor = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSalvar(nome, descricao.ifBlank { null }, cor) },
            ) { Text(text = "Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDispensar) { Text(text = "Cancelar") }
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
