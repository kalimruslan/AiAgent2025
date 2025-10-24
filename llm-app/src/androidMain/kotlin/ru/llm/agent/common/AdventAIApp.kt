package ru.llm.agent.common

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.llm.agent.BuildConfig
import ru.llm.agent.common.app.initKoinApp

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
