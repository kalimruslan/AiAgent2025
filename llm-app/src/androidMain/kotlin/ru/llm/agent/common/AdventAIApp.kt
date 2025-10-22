package ru.llm.agent

import android.app.Application
import ru.llm.agent.common.initKoinApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AdventAIApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AdventAIApp)
            initKoinApp(
                platformContext = this@AdventAIApp,
                isDebug = BuildConfig.DEBUG,
            )
        }
    }
}
