package com.example.autodialer.utils.extensions

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager


fun Context.subscribeToCallEndCallback(
    callback: () -> Unit
) {

    val telephonyManager: TelephonyManager =
        getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    var lastState = TelephonyManager.CALL_STATE_IDLE

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        telephonyManager.registerTelephonyCallback(
            mainExecutor,
            object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (lastState == state) return
                    when (state) {
                        TelephonyManager.CALL_STATE_IDLE -> {
                            callback()
                        }
                    }
                    lastState = state
                }
            })
    } else {
        telephonyManager.listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                if (lastState == state) return
                when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        callback()
                    }
                }
                lastState = state
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }
}

