package br.com.revisaciclo.ui.configuracoes

import br.com.revisaciclo.ui.common.localizarMensagem
import br.com.revisaciclo.R
import br.com.revisaciclo.ui.common.recursosLocalizados

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.revisaciclo.domain.backup.BackupFormato
import br.com.revisaciclo.domain.backup.ResumoBackup
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.TemaApp
import br.com.revisaciclo.domain.preferencias.ResultadoIntervalos
import br.com.revisaciclo.domain.preferencias.ValidadorIntervalos
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onVoltar: () -> Unit,
    viewModel: ConfiguracoesViewModel = viewModel(factory = ConfiguracoesViewModel.Factory),
    backupViewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory),
) {
    val textos = recursosLocalizados()
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val estadoBackup by backupViewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var dialogoHorario by remember { mutableStateOf(false) }
    var dialogoIntervalos by remember { mutableStateOf(false) }
    var dialogoExportar by remember { mutableStateOf(false) }

    val lancadorPermissao = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { concedida ->
        viewModel.definirNotificacoesAtivas(concedida)
    }

    val lancadorSalvarBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupFormato.MIME),
    ) { uri -> uri?.let { backupViewModel.exportarBackup(it.toString()) } }

    val lancadorSalvarJson = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupFormato.MIME),
    ) { uri -> uri?.let { backupViewModel.exportarJson(it.toString()) } }

    val lancadorAbrirBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { backupViewModel.analisarBackup(it.toString()) } }

    val mensagemBackup = estadoBackup.mensagem
    LaunchedEffect(mensagemBackup) {
        if (mensagemBackup != null) {
            snackbarHostState.showSnackbar(textos.localizarMensagem(mensagemBackup))
            backupViewModel.limparMensagem()
        }
    }

    val compartilharUri = estadoBackup.compartilhar
    LaunchedEffect(compartilharUri) {
        if (compartilharUri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = BackupFormato.MIME
                putExtra(Intent.EXTRA_STREAM, compartilharUri.toUri())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            contexto.startActivity(Intent.createChooser(intent, textos.getString(R.string.share_backup)))
            backupViewModel.consumirCompartilhamento()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = textos.getString(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = textos.getString(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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

            SecaoBackup(
                ocupado = estadoBackup.ocupado,
                onExportar = { dialogoExportar = true },
                onCompartilhar = backupViewModel::compartilharBackup,
                onRestaurar = {
                    lancadorAbrirBackup.launch(
                        arrayOf(BackupFormato.MIME, "application/octet-stream", "*/*"),
                    )
                },
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

    if (dialogoExportar) {
        DialogoExportar(
            onBackup = {
                dialogoExportar = false
                lancadorSalvarBackup.launch(nomeArquivoBackup(backupViewModel.hoje))
            },
            onJson = {
                dialogoExportar = false
                lancadorSalvarJson.launch(nomeArquivoJson(backupViewModel.hoje))
            },
            onDispensar = { dialogoExportar = false },
        )
    }

    estadoBackup.previa?.let { previa ->
        DialogoConfirmarRestauracao(
            resumo = previa.resumo,
            restaurando = estadoBackup.restaurando,
            onConfirmar = backupViewModel::confirmarRestauracao,
            onCancelar = backupViewModel::descartarPrevia,
        )
    }

    if (estadoBackup.analisando) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(text = textos.getString(R.string.reading_backup)) },
            text = { Text(text = textos.getString(R.string.checking_file_integrity)) },
        )
    }

    if (estadoBackup.restaurando) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(text = textos.getString(R.string.restoring)) },
            text = { Text(text = textos.getString(R.string.replacing_data_keep_the_app_open)) },
        )
    }
}

private fun nomeArquivoBackup(hoje: LocalDate): String {
    return BackupFormato.nomeSugerido(hoje.year, hoje.monthValue, hoje.dayOfMonth)
}

private fun nomeArquivoJson(hoje: LocalDate): String {
    return "revisaciclo-export-%04d%02d%02d.json".format(
        hoje.year,
        hoje.monthValue,
        hoje.dayOfMonth,
    )
}

@Composable
private fun SecaoBackup(
    ocupado: Boolean,
    onExportar: () -> Unit,
    onCompartilhar: () -> Unit,
    onRestaurar: () -> Unit,
) {
    val textos = recursosLocalizados()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = textos.getString(R.string.backup_and_data), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = textos.getString(R.string.create_a_full_backup_to_save_or_transfer_your_data_restoring_repl),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onExportar,
                    enabled = !ocupado,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = textos.getString(R.string.export))
                }
                OutlinedButton(
                    onClick = onRestaurar,
                    enabled = !ocupado,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = textos.getString(R.string.restore))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCompartilhar,
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = textos.getString(R.string.share_backup))
            }
        }
    }
}

@Composable
private fun DialogoExportar(
    onBackup: () -> Unit,
    onJson: () -> Unit,
    onDispensar: () -> Unit,
) {
    val textos = recursosLocalizados()
    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = textos.getString(R.string.export_data)) },
        text = {
            Column {
                Text(
                    text = textos.getString(R.string.choose_the_export_format),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = textos.getString(R.string.full_backup_revisaciclo_can_be_restored_by_the_app),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = textos.getString(R.string.portable_json_json_open_format_for_use_in_other_tools),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onBackup) { Text(text = textos.getString(R.string.full_backup)) }
        },
        dismissButton = {
            TextButton(onClick = onJson) { Text(text = textos.getString(R.string.portable_json)) }
        },
    )
}

@Composable
private fun DialogoConfirmarRestauracao(
    resumo: ResumoBackup,
    restaurando: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val textos = recursosLocalizados()
    val data = remember(resumo.criadoEm) {
        java.time.format.DateTimeFormatter
            .ofPattern(if (java.util.Locale.getDefault().language == "en" && java.util.Locale.getDefault().country == "US") "MM/dd/yyyy HH:mm" else "dd/MM/yyyy HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
            .format(resumo.criadoEm)
    }
    AlertDialog(
        onDismissRequest = { if (!restaurando) onCancelar() },
        title = { Text(text = textos.getString(R.string.restore_backup)) },
        text = {
            Column {
                Text(
                    text = textos.getString(R.string.this_replaces_all_current_data_with_the_file_contents_a_safety_co),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = textos.getString(R.string.created_on_format, data), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = textos.getString(R.string.projects_format, resumo.projetos),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = textos.getString(R.string.subjects_format, resumo.disciplinas),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = textos.getString(R.string.topics_format, resumo.assuntos),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = textos.getString(R.string.reviews_format, resumo.revisoes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar, enabled = !restaurando) {
                Text(text = textos.getString(R.string.restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !restaurando) {
                Text(text = textos.getString(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SecaoNotificacoes(
    preferencias: Preferencias,
    onAtivar: (Boolean) -> Unit,
    onEscolherHorario: () -> Unit,
) {
    val textos = recursosLocalizados()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = textos.getString(R.string.notifications),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = textos.getString(R.string.review_reminders), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = textos.getString(R.string.notifies_you_when_reviews_are_due_today),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = preferencias.notificacoesAtivas,
                    onCheckedChange = onAtivar,
                    modifier = Modifier.semantics {
                        contentDescription = textos.getString(R.string.review_reminders)
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = textos.getString(R.string.preferred_time), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = formatarHorario(preferencias.horarioNotificacao),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedButton(onClick = onEscolherHorario) {
                    Text(text = textos.getString(R.string.change))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = textos.getString(R.string.android_may_delay_reminders_to_save_battery_doze_the_exact_time_i),
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
    val textos = recursosLocalizados()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = textos.getString(R.string.daily_goal), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = textos.getString(R.string.reviews_you_aim_to_complete_each_day),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = textos.getString(R.string.reviews_format_2, metaDiaria),
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
    val textos = recursosLocalizados()
    val opcoes = listOf(
        TemaApp.CLARO to textos.getString(R.string.light),
        TemaApp.ESCURO to textos.getString(R.string.dark),
        TemaApp.AUTOMATICO to textos.getString(R.string.system_default),
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = textos.getString(R.string.theme), style = MaterialTheme.typography.titleMedium)
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
    val textos = recursosLocalizados()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = textos.getString(R.string.review_intervals), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = textos.getString(R.string.days_between_reviews_format, ValidadorIntervalos.formatar(intervalos)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = textos.getString(R.string.after_the_final_stage_reviews_keep_using_the_last_interval_changi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onEditar) {
                Text(text = textos.getString(R.string.edit_intervals))
            }
        }
    }
}

@Composable
private fun SecaoSobre(versao: String) {
    val textos = recursosLocalizados()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = textos.getString(R.string.about), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "RevisaCiclo", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = textos.getString(R.string.version_format, versao),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = textos.getString(R.string.offline_spaced_repetition_your_data_stays_on_your_device),
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
    val textos = recursosLocalizados()
    val estado = rememberTimePickerState(
        initialHour = horario.hour,
        initialMinute = horario.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = textos.getString(R.string.preferred_time)) },
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = { onConfirmar(estado.hour, estado.minute) }) {
                Text(text = textos.getString(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDispensar) { Text(text = textos.getString(R.string.cancel)) }
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
    val textos = recursosLocalizados()
    var texto by remember(intervalosAtuais) {
        mutableStateOf(ValidadorIntervalos.formatar(intervalosAtuais))
    }
    val erro = remember(texto) {
        (ValidadorIntervalos.deTexto(texto) as? ResultadoIntervalos.Invalido)?.motivo?.mensagem
    }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text(text = textos.getString(R.string.review_intervals)) },
        text = {
            Column {
                Text(
                    text = textos.getString(R.string.enter_the_days_between_reviews_separated_by_commas_values_must_be),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true,
                    isError = erro != null,
                    label = { Text(text = textos.getString(R.string.e_g_1_3_7_15_30)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (erro != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = textos.localizarMensagem(erro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(texto) }) { Text(text = textos.getString(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onRestaurar) { Text(text = textos.getString(R.string.reset_to_default)) }
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
