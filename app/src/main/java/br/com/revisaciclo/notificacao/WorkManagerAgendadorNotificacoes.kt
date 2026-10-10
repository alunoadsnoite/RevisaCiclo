package br.com.revisaciclo.notificacao

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.notificacao.AgendaNotificacao
import br.com.revisaciclo.domain.notificacao.AgendadorNotificacoes
import java.time.Clock
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Agendador baseado em WorkManager.
 *
 * Usamos um trabalho único de execução curta com atraso até o próximo horário
 * preferido; ao concluir, o próprio worker reprograma o dia seguinte. Isso dá
 * controle sobre o horário preferido sem alarmes exatos e sem serviços
 * permanentes. O WorkManager persiste o trabalho e o reprograma após
 * reinicialização do dispositivo.
 */
class WorkManagerAgendadorNotificacoes(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) : AgendadorNotificacoes {

    override fun garantirAgendado(preferencias: Preferencias) {
        if (!preferencias.notificacoesAtivas) return
        enfileirar(preferencias, ExistingWorkPolicy.KEEP)
    }

    override fun reagendar(preferencias: Preferencias) {
        if (!preferencias.notificacoesAtivas) {
            cancelar()
            return
        }
        enfileirar(preferencias, ExistingWorkPolicy.REPLACE)
    }

    override fun cancelar() {
        WorkManager.getInstance(context).cancelUniqueWork(NOME_TRABALHO)
    }

    private fun enfileirar(preferencias: Preferencias, politica: ExistingWorkPolicy) {
        val atraso = AgendaNotificacao.atraso(
            horario = preferencias.horarioNotificacao,
            agora = ZonedDateTime.now(clock),
        )
        val requisicao = OneTimeWorkRequestBuilder<VerificadorRevisoesWorker>()
            .setInitialDelay(atraso.toMillis().coerceAtLeast(1L), TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOME_TRABALHO, politica, requisicao)
    }

    companion object {
        const val NOME_TRABALHO = "verificacao_revisoes"
        const val TAG = "revisoes"
    }
}
