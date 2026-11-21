package uz.inglish.app

import android.app.Application
import uz.inglish.app.core.AppContainer

class InglishApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
