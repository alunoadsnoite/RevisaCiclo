package br.com.revisaciclo.notificacao

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.revisaciclo.RevisaCicloApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reagenda a verificação após eventos de sistema que podem desalinhar o horário
 * preferido: reinicialização, mudança de data e mudança de fuso.
 *
 * É um [BroadcastReceiver] de execução curta (não é um serviço permanente). O
 * WorkManager já reprograma trabalhos persistidos após reboot; este receiver
 * garante o realinhamento também em mudanças de hora/fuso.
 */
class ReagendadorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> Unit

            else -> return
        }

        val app = context.applicationContext as? RevisaCicloApp ?: return
        val resultado = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val preferencias = app.container.preferenciasRepository.preferencias.first()
                app.container.agendadorNotificacoes.reagendar(preferencias)
            } finally {
                resultado.finish()
            }
        }
    }
}
