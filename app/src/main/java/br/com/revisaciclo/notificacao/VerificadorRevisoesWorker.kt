package br.com.revisaciclo.notificacao

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.revisaciclo.RevisaCicloApp
import br.com.revisaciclo.domain.notificacao.DecisorNotificacao
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

/**
 * Verifica em segundo plano as revisões devidas e publica (no máximo) uma
 * notificação por dia. O trabalho é de execução curta e reprograma a próxima
 * verificação para o horário preferido do dia seguinte.
 *
 * Não usa alarmes exatos; depende do agendamento do WorkManager, que respeita
 * Doze e restrições de bateria e não garante execução no minuto exato.
 */
class VerificadorRevisoesWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RevisaCicloApp ?: return Result.success()
        val container = app.container

        return try {
            val preferencias = container.preferenciasRepository.preferencias.first()
            if (preferencias.notificacoesAtivas) {
                val hoje = LocalDate.now(container.clock)
                val devidas = container.revisaoRepository.contarRevisoesDevidas(hoje)
                val jaNotificado = container.preferenciasRepository.ultimaNotificacaoDia() == hoje.toEpochDay()
                val agora = ZonedDateTime.now(container.clock)

                if (DecisorNotificacao.deveNotificar(preferencias, devidas, jaNotificado, agora)) {
                    container.notificadorRevisoes.notificar(devidas)
                    container.preferenciasRepository.registrarNotificacaoDia(hoje.toEpochDay())
                }
            }
            container.agendadorNotificacoes.reagendar(preferencias)
            Result.success()
        } catch (erro: Exception) {
            Result.retry()
        }
    }
}
