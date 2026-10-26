package br.com.revisaciclo.ui.projetos

import br.com.revisaciclo.R
import br.com.revisaciclo.ui.common.recursosLocalizados

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Seletor compacto do projeto ativo exibido nas barras superiores.
 *
 * Mostra o nome do projeto ativo e permite trocar rapidamente ou abrir a tela
 * de gerenciamento de projetos.
 */
@Composable
fun SeletorProjetoAtivo(
    onGerenciarProjetos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjetosViewModel = viewModel(factory = ProjetosViewModel.Factory),
) {
    val textos = recursosLocalizados()
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var aberto by remember { mutableStateOf(false) }
    val ativo = estado.projetoAtivo

    Box(modifier = modifier) {
        TextButton(onClick = { aberto = true }) {
            Text(
                text = estado.projetoAtivo?.nome ?: textos.getString(R.string.projects),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            estado.ativos.forEach { projeto ->
                DropdownMenuItem(
                    text = { Text(text = projeto.nome) },
                    leadingIcon = if (projeto.id == estado.projetoAtivo?.id) {
                        { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        aberto = false
                        viewModel.selecionar(projeto.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = textos.getString(R.string.manage_projects)) },
                leadingIcon = {
                    Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null)
                },
                onClick = {
                    aberto = false
                    onGerenciarProjetos()
                },
            )
        }
    }
}
