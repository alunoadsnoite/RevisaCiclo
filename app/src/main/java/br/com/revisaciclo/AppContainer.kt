package br.com.revisaciclo

import android.content.Context
import br.com.revisaciclo.data.backup.ArmazenamentoBackupRoom
import br.com.revisaciclo.data.backup.ArmazenamentoSegurancaArquivos
import br.com.revisaciclo.data.backup.FonteArquivoAndroid
import br.com.revisaciclo.data.backup.PublicadorArquivo
import br.com.revisaciclo.data.backup.criarEstadoOperacaoRepository
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.preferencias.criarPreferenciasRepository
import br.com.revisaciclo.data.repository.OfflineProjetoRepository
import br.com.revisaciclo.data.repository.OfflineRevisaoRepository
import br.com.revisaciclo.domain.backup.ArmazenamentoBackup
import br.com.revisaciclo.domain.backup.ArmazenamentoSeguranca
import br.com.revisaciclo.domain.backup.FonteArquivo
import br.com.revisaciclo.domain.backup.ReconciliadorRestauracao
import br.com.revisaciclo.domain.backup.RepositorioEstadoOperacao
import br.com.revisaciclo.domain.backup.ServicoBackup
import br.com.revisaciclo.domain.backup.ServicoRestauracao
import br.com.revisaciclo.domain.notificacao.AgendadorNotificacoes
import br.com.revisaciclo.domain.notificacao.GerenciadorNotificacoes
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.ProjetoRepository
import br.com.revisaciclo.domain.repository.RevisaoRepository
import br.com.revisaciclo.notificacao.NotificadorRevisoes
import br.com.revisaciclo.notificacao.WorkManagerAgendadorNotificacoes
import java.io.File
import java.time.Clock

class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val database: RevisaCicloDatabase by lazy {
        RevisaCicloDatabase.getInstance(appContext)
    }

    val clock: Clock = Clock.systemDefaultZone()

    val versaoApp: String by lazy {
        @Suppress("DEPRECATION")
        runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        }.getOrNull() ?: "0.0.0"
    }

    val preferenciasRepository: PreferenciasRepository by lazy {
        criarPreferenciasRepository(appContext)
    }

    private val estadoOperacaoRepository: RepositorioEstadoOperacao by lazy {
        criarEstadoOperacaoRepository(appContext)
    }

    private val fonteArquivo: FonteArquivo by lazy {
        FonteArquivoAndroid(appContext.contentResolver)
    }

    val publicadorArquivo: PublicadorArquivo by lazy {
        PublicadorArquivo(appContext)
    }

    private val armazenamentoSeguranca: ArmazenamentoSeguranca by lazy {
        ArmazenamentoSegurancaArquivos(File(appContext.filesDir, "backup_seguranca"))
    }

    private val armazenamentoBackup: ArmazenamentoBackup by lazy {
        ArmazenamentoBackupRoom(database, preferenciasRepository)
    }

    val gerenciadorNotificacoes: GerenciadorNotificacoes by lazy {
        GerenciadorNotificacoes(agendadorNotificacoes)
    }

    val servicoBackup: ServicoBackup by lazy {
        ServicoBackup(armazenamentoBackup, fonteArquivo, versaoApp, clock)
    }

    val servicoRestauracao: ServicoRestauracao by lazy {
        ServicoRestauracao(
            armazenamento = armazenamentoBackup,
            estadoOperacao = estadoOperacaoRepository,
            fonteArquivo = fonteArquivo,
            armazenamentoSeguranca = armazenamentoSeguranca,
            gerenciadorNotificacoes = gerenciadorNotificacoes,
            versaoApp = versaoApp,
            clock = clock,
        )
    }

    val reconciliadorRestauracao: ReconciliadorRestauracao by lazy {
        ReconciliadorRestauracao(estadoOperacaoRepository, armazenamentoBackup, gerenciadorNotificacoes)
    }

    val revisaoRepository: RevisaoRepository by lazy {
        OfflineRevisaoRepository(database, preferenciasRepository, clock)
    }

    val projetoRepository: ProjetoRepository by lazy {
        OfflineProjetoRepository(database, preferenciasRepository, clock)
    }

    val agendadorNotificacoes: AgendadorNotificacoes by lazy {
        WorkManagerAgendadorNotificacoes(appContext, clock)
    }

    val notificadorRevisoes: NotificadorRevisoes by lazy {
        NotificadorRevisoes(appContext)
    }
}
