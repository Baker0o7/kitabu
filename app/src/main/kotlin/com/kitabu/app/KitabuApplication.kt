package com.kitabu.app

import android.app.Application
import com.kitabu.app.data.KitabuDatabase
import com.kitabu.app.util.TrashWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KitabuApplication : Application() {

    val database by lazy { KitabuDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        TrashWorker.schedule(this)
    }
}
