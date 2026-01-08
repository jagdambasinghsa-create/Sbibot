package com.customersupport.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SimManager - Handles dual SIM detection and SIM card information retrieval.
 * Uses SubscriptionManager API (Android 5.1+) to get active SIM subscriptions.
 */
@Singleton
class SimManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SimManager"
    }

    /**
     * Get all active SIM cards with their details.
     * Returns a JSONArray containing SIM info objects.
     */
    fun getSimCards(): JSONArray {
        val simArray = JSONArray()

        // Check READ_PHONE_STATE permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "READ_PHONE_STATE permission not granted - cannot read SIM info")
            return simArray
        } else {
            Log.d(TAG, "READ_PHONE_STATE permission granted")
        }

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            
            if (subscriptionManager == null) {
                Log.e(TAG, "SubscriptionManager is null")
                return simArray
            }

            val activeSubscriptions: List<SubscriptionInfo>? = try {
                subscriptionManager.activeSubscriptionInfoList
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException accessing activeSubscriptionInfoList", e)
                null
            }

            Log.d(TAG, "activeSubscriptionInfoList returned: ${activeSubscriptions?.size ?: "null"} items")

            if (activeSubscriptions.isNullOrEmpty()) {
                Log.w(TAG, "No active SIM subscriptions found - list is ${if (activeSubscriptions == null) "null" else "empty"}")
                return simArray
            }

            for (subscriptionInfo in activeSubscriptions) {
                val simInfo = JSONObject().apply {
                    put("slotIndex", subscriptionInfo.simSlotIndex)
                    put("subscriptionId", subscriptionInfo.subscriptionId)
                    put("carrierName", subscriptionInfo.carrierName?.toString() ?: "Unknown Carrier")
                    put("displayName", subscriptionInfo.displayName?.toString() ?: "SIM ${subscriptionInfo.simSlotIndex + 1}")
                    
                    // Get phone number - may be empty depending on carrier/device
                    val phoneNumber = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            subscriptionManager.getPhoneNumber(subscriptionInfo.subscriptionId)
                        } else {
                            @Suppress("DEPRECATION")
                            subscriptionInfo.number
                        }
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Permission denied for getting phone number", e)
                        ""
                    } catch (e: Exception) {
                        Log.w(TAG, "Error getting phone number", e)
                        ""
                    }
                    put("phoneNumber", phoneNumber ?: "")
                    
                    // Additional info
                    put("countryIso", subscriptionInfo.countryIso ?: "")
                }
                simArray.put(simInfo)
                Log.d(TAG, "Found SIM: slot=${subscriptionInfo.simSlotIndex}, carrier=${subscriptionInfo.carrierName}")
            }

            Log.d(TAG, "Total SIM cards found: ${simArray.length()}")

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for reading SIM info", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SIM info", e)
        }

        return simArray
    }

    /**
     * Get the number of active SIM cards.
     */
    fun getSimCount(): Int {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            @Suppress("MissingPermission")
            subscriptionManager.activeSubscriptionInfoCount
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SIM count", e)
            0
        }
    }

    /**
     * Check if device has dual SIM capability.
     */
    fun isDualSim(): Boolean {
        return getSimCount() > 1
    }

    /**
     * Get subscription ID for a specific SIM slot (0 or 1).
     * Returns -1 if not found.
     */
    fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            @Suppress("MissingPermission")
            val subscriptions = subscriptionManager.activeSubscriptionInfoList ?: return -1
            subscriptions.find { it.simSlotIndex == slotIndex }?.subscriptionId ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscription ID for slot $slotIndex", e)
            -1
        }
    }
}
