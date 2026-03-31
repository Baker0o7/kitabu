package com.kitabu.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kitabu.app.data.KitabuDatabase
import com.kitabu.app.util.TrashWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KitabuApplication : Application(), Configuration.Provider {

    val database by lazy { KitabuDatabase.getDatabase(this) }

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        TrashWorker.schedule(this)
    }
}
