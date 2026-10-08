package com.example.musikku

import android.app.Application
import com.example.musikku.di.AppContainer

class MusikKuApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Extension untuk akses AppContainer dari Context mana pun. */
val android.content.Context.appContainer: AppContainer
    get() = (applicationContext as MusikKuApp).container
