package com.familysms.forwarder

import android.app.Application
import com.familysms.forwarder.data.local.AppDatabase
import com.familysms.forwarder.data.preferences.SettingsRepository
import com.familysms.forwarder.data.repository.SmsForwarderRepository
import com.familysms.forwarder.util.NotificationHelper

class FamilySmsApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var repository: SmsForwarderRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        repository = SmsForwarderRepository(database, settingsRepository)
        NotificationHelper.createNotificationChannels(this)
    }
}
