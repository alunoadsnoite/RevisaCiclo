package br.com.revisaciclo

import android.app.Application

class RevisaCicloApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
