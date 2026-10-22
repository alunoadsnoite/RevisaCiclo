package br.com.revisaciclo

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.TemaApp
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Store images rendered from the real app with isolated, illustrative test data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en-rUS-w360dp-h800dp-xhdpi-notnight", application = RevisaCicloApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EnglishScreenshotsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun captureFiveEnglishScreens() {
        assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        val container = (compose.activity.application as RevisaCicloApp).container
        runBlocking(Dispatchers.IO) {
            val project = container.projetoRepository.observarProjetos().first { it.isNotEmpty() }.first()
            container.projetoRepository.atualizar(project.id, "My studies", null, project.cor)
            container.preferenciasRepository.atualizar { it.copy(projetoAtivoId = project.id, tema = TemaApp.CLARO) }
            val repo = container.revisaoRepository
            val math = repo.criarDisciplina(project.id, "Mathematics", 0xFF1565C0L)
            val computing = repo.criarDisciplina(project.id, "Computer science", 0xFF2E7D32L)
            val today = LocalDate.now(container.clock)
            repo.criarAssunto(math, "Algebra", "Equations and functions", today.minusDays(5))
            repo.criarAssunto(math, "Probability", "Independent events", today.minusDays(1))
            repo.criarAssunto(computing, "Data structures", "Lists, trees and graphs", today.minusDays(3))
            val algorithms = repo.criarAssunto(computing, "Algorithms", "Complexity and sorting", today.minusDays(8))
            repeat(2) {
                val review = repo.observarRevisoesPendentes().first().first { it.assuntoId == algorithms }
                repo.concluirRevisao(review.revisaoId, AvaliacaoRevisao.BOM)
            }
        }
        waitForText("Mathematics")
        capture("1-dashboard.png")
        navigate("Subjects")
        waitForText("Computer science")
        capture("2-subjects.png")
        navigate("Reviews")
        waitForText("Algebra")
        capture("3-reviews.png")
        navigate("Statistics")
        waitForText("Active project")
        capture("4-statistics.png")
        navigate("Stages")
        waitForText("Start review session")
        capture("5-stages.png")
    }

    private fun navigate(label: String) {
        compose.onNode(hasText(label) and hasClickAction()).performClick()
        compose.waitForIdle()
    }

    private fun waitForText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun capture(name: String) {
        val output = File("build/outputs/english-screenshots").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(output, name).path)
    }
}
