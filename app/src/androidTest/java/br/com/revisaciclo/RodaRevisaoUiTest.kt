package br.com.revisaciclo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.revisaciclo.ui.roda.RodaRevisaoScreen
import br.com.revisaciclo.ui.roda.RodaRevisaoViewModel
import br.com.revisaciclo.ui.theme.RevisaCicloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RodaRevisaoUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun rodaExibeEstagiosNaLegenda() {
        val app = compose.activity.application as RevisaCicloApp
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

        compose.onNodeWithText("Estágios").assertIsDisplayed()
        compose.onNodeWithText("1 dia").assertIsDisplayed()
        compose.onNodeWithText("3 dias").assertIsDisplayed()
        compose.onNodeWithText("7 dias").assertIsDisplayed()
        compose.onNodeWithText("15 dias").assertIsDisplayed()
        compose.onNodeWithText("30 dias").assertIsDisplayed()
    }
}