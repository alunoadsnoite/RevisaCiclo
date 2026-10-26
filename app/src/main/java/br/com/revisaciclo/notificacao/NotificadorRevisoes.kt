package br.com.revisaciclo.notificacao

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.revisaciclo.MainActivity
import br.com.revisaciclo.R
import br.com.revisaciclo.navigation.Destinos

/**
 * Publica a notificação de revisões pendentes. Ao tocar, abre o app na aba de
 * Revisões. Não notifica se a permissão POST_NOTIFICATIONS não estiver concedida
 * (Android 13+).
 */
class NotificadorRevisoes(private val context: Context) {

    fun temPermissao(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun notificar(quantidade: Int) {
        if (!temPermissao()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_DESTINO, Destinos.REVISOES)
        }
        val pendente = PendingIntent.getActivity(
            context,
            CODIGO_INTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val texto = context.resources.getQuantityString(
            R.plurals.reviews_due_notification, quantidade, quantidade,
        )

        val notificacao = NotificationCompat.Builder(context, CanalNotificacao.ID)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(context.getString(R.string.review_notification_title))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(pendente)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(CODIGO_NOTIFICACAO, notificacao)
    }

    companion object {
        private const val CODIGO_NOTIFICACAO = 1001
        private const val CODIGO_INTENT = 2001
    }
}
