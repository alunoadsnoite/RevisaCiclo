package br.com.revisaciclo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.revisaciclo.ui.roda.RodaRevisaoScreen
import br.com.revisaciclo.ui.roda.RodaRevisaoViewModel
import br.com.revisaciclo.ui.theme.RevisaCicloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RodaRevisaoUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: RevisaCicloApp
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as RevisaCicloApp

    private fun exibirRoda() {
        val viewModel = RodaRevisaoViewModel(
            repository = app.container.revisaoRepository,
            preferenciasRepository = app.container.preferenciasRepository,
            projetoRepository = app.container.projetoRepository,
            clock = app.container.clock,
        )

        compose.setContent {
            RevisaCicloTheme {
                RodaRevisaoScreen(onIniciarSessao = {}, viewModel = viewModel)
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("1 dia").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun rodaExibeEstagiosNaLegenda() {
        exibirRoda()

        compose.onAllNodesWithText("Estágios").onFirst().assertIsDisplayed()
        compose.onNodeWithText("1 dia").assertIsDisplayed()
        compose.onNodeWithText("3 dias").assertIsDisplayed()
        compose.onNodeWithText("7 dias").assertIsDisplayed()
        compose.onNodeWithText("15 dias").assertIsDisplayed()
        compose.onNodeWithText("30 dias").assertIsDisplayed()
    }

    @Test
    fun rodaExpoeDescricaoAcessivel() {
        exibirRoda()

        compose.onNodeWithContentDescription("Roda de revisão", substring = true)
            .assertIsDisplayed()
    }
}
