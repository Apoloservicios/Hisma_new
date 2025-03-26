package com.hisma.app

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HismaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar Firebase explícitamente
        FirebaseApp.initializeApp(this)
    }
}