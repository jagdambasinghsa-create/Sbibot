package com.customersupport

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.customersupport.data.PreferencesManager
import com.customersupport.socket.SocketManager

class CustomerSupportApp : Application() {

    companion object {
        const val CHANNEL_ID = "socket_service_channel"
        const val CHANNEL_NAME = "Customer Support Service"
        
        // Manual singleton instances (replacing Hilt)
        lateinit var instance: CustomerSupportApp
            private set
        
        val socketManager: SocketManager by lazy { SocketManager() }
        val preferencesManager: PreferencesManager by lazy { PreferencesManager(instance) }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background service for syncing data"
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
