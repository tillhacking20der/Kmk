package com.example

import android.app.Application
import com.example.data.local.JarvisDatabase
import com.example.data.repository.JarvisRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JarvisApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { JarvisDatabase.getDatabase(this) }
    val repository by lazy { JarvisRepository(this, database.customCommandDao()) }
    val settingsRepository by lazy { SettingsRepository.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.seedInitialCommandsIfNeeded()
        }
    }
}
