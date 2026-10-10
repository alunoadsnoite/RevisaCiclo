package br.com.revisaciclo.ui.configuracoes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.TemaApp
import br.com.revisaciclo.domain.preferencias.ResultadoIntervalos
import br.com.revisaciclo.domain.preferencias.ValidadorIntervalos
import java.time.LocalTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onVoltar: () -> Unit,
    viewModel: ConfiguracoesViewModel = viewModel(factory = ConfiguracoesViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var dialogoHorario by remember { mutableStateOf(false) }
    var dialogoIntervalos by remember { mutableStateOf(false) }

    val lancadorPermissao = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { concedida ->
        viewModel.definirNotificacoesAtivas(concedida)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Configurações") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SecaoNotificacoes(
                preferencias = estado.preferencias,
                onAtivar = { ativo ->
                    if (ativo && !permissaoConcedida(contexto)) {
                        lancadorPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.definirNotificacoesAtivas(ativo)
                    }
                },
                onEscolherHorario = { dialogoHorario = true },
            )

            SecaoMetaDiaria(
                metaDiaria = estado.preferencias.metaDiaria,
                onAlterar = viewModel::definirMetaDiaria,
            )

            SecaoTema(
                tema = estado.preferencias.tema,
                onSelecionar = viewModel::definirTema,
            )

            SecaoIntervalos(
                intervalos = estado.preferencias.intervalosDias,
                onEditar = { dialogoIntervalos = true },
            )

            SecaoSobre(versao = versaoDoApp(contexto))
        }
    }

    if (dialogoHorario) {
        DialogoHorario(
            horario = estado.preferencias.horarioNotificacao,
            onConfirmar = { hora, minuto ->
                viewModel.definirHorario(hora, minuto)
                dialogoHorario = false
            },
            onDispensar = { dialogoHorario = false },
        )
    }

    if (dialogoIntervalos) {
        DialogoIntervalos(
            intervalosAtuais = estado.preferencias.intervalosDias,
            onSalvar = { texto ->
                if (viewModel.salvarIntervalos(texto)) {
                    dialogoIntervalos = false
                }
            },
            onRestaurar = { viewModel.restaurarIntervalosPadrao() },
            onDispensar = {
                dialogoIntervalos = false
                viewModel.limparErroIntervalos()
            },
        )
    }
}

@Composable
private fun SecaoNotificacoes(
    preferencias: Preferencias,
    onAtivar: (Boolean) -> Unit,
    onEscolherHorario: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Notificações",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Lembretes de revisão", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Avisa quando houver revisões pendentes no dia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = preferencias.notificacoesAtivas,
                    onCheckedChange = onAtivar,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Horário preferido", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = formatarHorario(preferencias.horarioNotificacao),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedButton(onClick = onEscolherHorario) {
                    Text(text = "Alterar")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "O Android pode atrasar lembretes para economizar bateria (Doze). " +
                    "Não garantimos o horário exato.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecaoMetaDiaria(
    metaDiaria: Int,
    onAlterar: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Meta diária", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Revisões que você pretende concluir por dia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$metaDiaria revisões",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Slider(
                value = metaDiaria.toFloat(),
                onValueChange = { onAlterar(it.roundToInt()) },
                valueRange = Preferencias.META_DIARIA_MINIMA.toFloat()..
                    Preferencias.META_DIARIA_MAXIMA.toFloat(),
                steps = 0,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecaoTema(
    tema: TemaApp,
    onSelecionar: (TemaApp) -> Unit,
) {
    val opcoes = listOf(
        TemaApp.CLARO to "Claro",
        TemaApp.ESCURO to "Escuro",
        TemaApp.AUTOMATICO to "Automático",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Tema", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                opcoes.forEachIndexed { indice, (valor, rotulo) ->
                    SegmentedButton(
                        selected = tema == valor,
                        onClick = { onSelecionar(valor) },
                        shape = SegmentedButtonDefaults.itemShape(index = indice, count = opcoes.size),
                    ) {
                        Text(text = rotulo)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecaoIntervalos(
    intervalos: List<Int>,
    onEditar: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Intervalos do ciclo", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dias entre revisões: ${ValidadorIntervalos.formatar(intervalos)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Após o último estágio, as revisões continuam no último intervalo. " +
                    "Alterar os intervalos não muda revisões já agendadas; vale para os " +
                    "próximos agendamentos e novos assuntos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onEditar) {
                Text(text = "Editar intervalos")
            }
        }
    }
}

@Composable
private fun SecaoSobre(versao: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Sobre", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "RevisaCiclo", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Versão $versao",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aplicativo offline de revisão espaçada. Seus dados ficam apenas no " +
                    "dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoHorario(
    horario: LocalTime,
    onConfirmar: (Int, Int) -> Unit,
    onDispensar: () -> Unit,
) {
    val estado = rememberTimePickerState(
        initialHour = horario.hour,
        initialMinute = horario.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = "Horário preferido") },
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = { onConfirmar(estado.hour, estado.minute) }) {
                Text(text = "Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDispensar) { Text(text = "Cancelar") }
        },
    )
}

@Composable
private fun DialogoIntervalos(
    intervalosAtuais: List<Int>,
    onSalvar: (String) -> Unit,
    onRestaurar: () -> Unit,
    onDispensar: () -> Unit,
) {
    var texto by remember(intervalosAtuais) {
        mutableStateOf(ValidadorIntervalos.formatar(intervalosAtuais))
    }
    val erro = remember(texto) {
        (ValidadorIntervalos.deTexto(texto) as? ResultadoIntervalos.Invalido)?.motivo?.mensagem
    }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = "Intervalos do ciclo") },
        text = {
            Column {
                Text(
                    text = "Informe os dias entre revisões, separados por vírgula. " +
                        "Precisam ser positivos e estritamente crescentes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true,
                    isError = erro != null,
                    label = { Text(text = "Ex.: 1, 3, 7, 15, 30") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (erro != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = erro,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(texto) }) { Text(text = "Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onRestaurar) { Text(text = "Restaurar padrão") }
        },
    )
}

private fun formatarHorario(horario: LocalTime): String =
    "%02d:%02d".format(horario.hour, horario.minute)

private fun permissaoConcedida(contexto: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

private fun versaoDoApp(contexto: Context): String =
    runCatching {
        contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName
    }.getOrNull() ?: "—"
