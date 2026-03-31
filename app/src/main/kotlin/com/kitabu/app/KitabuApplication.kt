package com.kitabu.app

import android.app.Application
import com.kitabu.app.data.KitabuDatabase

class KitabuApplication : Application() {
    val database by lazy { KitabuDatabase.getDatabase(this) }
}
