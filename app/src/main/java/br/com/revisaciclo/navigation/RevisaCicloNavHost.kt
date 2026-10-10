package br.com.revisaciclo.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.revisaciclo.ui.assuntos.AssuntosScreen
import br.com.revisaciclo.ui.configuracoes.ConfiguracoesScreen
import br.com.revisaciclo.ui.dashboard.DashboardScreen
import br.com.revisaciclo.ui.disciplinas.DisciplinasScreen
import br.com.revisaciclo.ui.estatisticas.EstatisticasScreen
import br.com.revisaciclo.ui.revisoes.RevisoesScreen
import br.com.revisaciclo.ui.roda.RodaRevisaoScreen
import br.com.revisaciclo.ui.sessao.SessaoRevisaoScreen

object Destinos {
    const val DASHBOARD = "dashboard"
    const val RODA = "roda"
    const val DISCIPLINAS = "disciplinas"
    const val REVISOES = "revisoes"
    const val ESTATISTICAS = "estatisticas"
    const val SESSAO = "sessao"
    const val CONFIGURACOES = "configuracoes"
    const val ASSUNTOS = "assuntos/{disciplinaId}"
    const val ARG_DISCIPLINA_ID = "disciplinaId"

    fun assuntos(disciplinaId: Long): String = "assuntos/$disciplinaId"
}

private data class Aba(
    val destino: String,
    val titulo: String,
)

@Composable
fun RevisaCicloNavHost(
    navController: NavHostController = rememberNavController(),
    destinoInicial: String? = null,
    onDestinoConsumido: () -> Unit = {},
) {
    LaunchedEffect(destinoInicial) {
        if (destinoInicial != null) {
            navController.navigate(destinoInicial) { launchSingleTop = true }
            onDestinoConsumido()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual = backStackEntry?.destination?.route
    val abas = listOf(
        Aba(Destinos.DASHBOARD, "Início") to Icons.Default.Home,
        Aba(Destinos.RODA, "Estágios") to Icons.Default.Refresh,
        Aba(Destinos.DISCIPLINAS, "Disciplinas") to Icons.AutoMirrored.Filled.List,
        Aba(Destinos.REVISOES, "Revisões") to Icons.Default.CheckCircle,
        Aba(Destinos.ESTATISTICAS, "Estatísticas") to Icons.Default.Info,
    )
    val mostrarBarra = abas.any { it.first.destino == rotaAtual }

    Scaffold(
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar {
                    abas.forEach { (aba, icone) ->
                        NavigationBarItem(
                            selected = rotaAtual == aba.destino,
                            onClick = {
                                if (rotaAtual != aba.destino) {
                                    navController.navigate(aba.destino) {
                                        popUpTo(Destinos.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(imageVector = icone, contentDescription = aba.titulo) },
                            label = { Text(text = aba.titulo) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinos.DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destinos.DASHBOARD) {
                DashboardScreen(
                    onAbrirConfiguracoes = { navController.navigate(Destinos.CONFIGURACOES) },
                )
            }
            composable(Destinos.RODA) {
                RodaRevisaoScreen(
                    onIniciarSessao = { navController.navigate(Destinos.SESSAO) },
                )
            }
            composable(Destinos.DISCIPLINAS) {
                DisciplinasScreen(
                    onAbrirAssuntos = { disciplinaId ->
                        navController.navigate(Destinos.assuntos(disciplinaId))
                    },
                )
            }
            composable(Destinos.REVISOES) {
                RevisoesScreen(
                    onIniciarSessao = { navController.navigate(Destinos.SESSAO) },
                )
            }
            composable(Destinos.ESTATISTICAS) {
                EstatisticasScreen()
            }
            composable(Destinos.SESSAO) {
                SessaoRevisaoScreen(
                    onSair = { navController.popBackStack() },
                )
            }
            composable(Destinos.CONFIGURACOES) {
                ConfiguracoesScreen(
                    onVoltar = { navController.popBackStack() },
                )
            }
            composable(
                route = Destinos.ASSUNTOS,
                arguments = listOf(
                    navArgument(Destinos.ARG_DISCIPLINA_ID) { type = NavType.LongType },
                ),
            ) { entry ->
                val disciplinaId = entry.arguments?.getLong(Destinos.ARG_DISCIPLINA_ID) ?: 0L
                AssuntosScreen(
                    disciplinaId = disciplinaId,
                    onVoltar = { navController.popBackStack() },
                )
            }
        }
    }
}