package ru.orangesoftware.financisto.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ru.orangesoftware.financisto.persistance.PreferencesStore

// A module with Kotlin and Java components
val modules = module {
//    singleOf(::KotlinComponent)
//    singleOf(::JavaComponent)
    single<PreferencesStore> {
        PreferencesStore(androidContext())
    }
}

// Start
fun start(myApplication: Application) {
    // Start Koin with given Application instance
    startKoin {
        // Log Koin into Android logger
        androidLogger()
        // Reference Android context
        androidContext(myApplication)
        // Load modules
        modules(listOf(modules))
    }
}

// Dependency holder
class DependenciesHolder : KoinComponent {
//    val kComp: KotlinComponent by inject()
//    val jComp: JavaComponent by inject()
    val preferencesStore: PreferencesStore by inject()
}
