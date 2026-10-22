package br.com.revisaciclo.notificacao

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import br.com.revisaciclo.R

/**
 * Canal de notificação do Android (obrigatório a partir do Android 8, que é o
 * minSdk deste app). Criado uma única vez na inicialização do processo.
 */
object CanalNotificacao {

    const val ID = "revisoes_pendentes"

    fun criar(context: Context) {
        val gerenciador = context.getSystemService(NotificationManager::class.java) ?: return
        val canal = NotificationChannel(
            ID,
            context.getString(R.string.review_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notifications_about_pending_reviews)
        }
        gerenciador.createNotificationChannel(canal)
    }
}
