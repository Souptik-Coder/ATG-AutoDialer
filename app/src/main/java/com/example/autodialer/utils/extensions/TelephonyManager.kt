package com.example.autodialer.utils.extensions

import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager


fun TelephonyManager.subscribeToCallEndCallback(
    callback: (String) -> Unit
) {
    var lastState = TelephonyManager.CALL_STATE_IDLE

    listen(object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (lastState == state) return
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    callback(phoneNumber.orEmpty())
                }
            }
            lastState = state
        }
    }, PhoneStateListener.LISTEN_CALL_STATE)
}

