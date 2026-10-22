package br.com.revisaciclo

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import br.com.revisaciclo.ui.common.formatarData
import br.com.revisaciclo.ui.common.localizarMensagem
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class LocalizationTest {
    private fun resources(tag: String): android.content.res.Resources {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        return context.createConfigurationContext(config).resources
    }

    @Test fun englishLabelsAndDefaultProjectAreAvailable() {
        val texts = resources("en-US")
        assertEquals("Subjects", texts.getString(R.string.subjects))
        assertEquals("Home", texts.getString(R.string.home))
        assertEquals("My studies", texts.getString(R.string.default_project_name))
    }

    @Test fun portugueseAndUnsupportedLocaleKeepPortugueseFallback() {
        for (tag in listOf("pt-BR", "fr-FR")) {
            val texts = resources(tag)
            assertEquals("Disciplinas", texts.getString(R.string.subjects))
            assertEquals("Início", texts.getString(R.string.home))
        }
    }

    @Test fun messagesPreserveCountsAndTranslateValidationDetails() {
        val texts = resources("en-US")
        assertEquals("Data restored. Topics: 2; reviews: 3.", texts.localizarMensagem("Dados restaurados: 2 assuntos, 3 revisões."))
        assertEquals("Backup validation failed: A topic has no title.", texts.localizarMensagem("O backup não pôde ser validado: Assunto sem título."))
        assertEquals("Intervals must be greater than zero.", texts.localizarMensagem("Os intervalos precisam ser maiores que zero."))
        assertEquals("Minhas anotações", texts.localizarMensagem("Minhas anotações"))
    }

    @Test fun notificationUsesSingularAndPlural() {
        val texts = resources("en-US")
        assertEquals("You have 1 review due today.", texts.getQuantityString(R.plurals.reviews_due_notification, 1, 1))
        assertEquals("You have 2 reviews due today.", texts.getQuantityString(R.plurals.reviews_due_notification, 2, 2))
    }

    @Test fun dateFormatFollowsTheSelectedLocale() {
        val day = LocalDate.of(2026, 10, 22)
        assertEquals("10/22/2026", formatarData(day, Locale.US))
        assertEquals("22/10/2026", formatarData(day, Locale.forLanguageTag("pt-BR")))
    }
}
