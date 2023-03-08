package com.example.autodialer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.autodialer.db.AppDatabase
import com.example.autodialer.models.User
import com.example.autodialer.utils.constants.NOTIFICATION_CHANNEL_ID
import com.example.autodialer.utils.constants.NOTIFICATION_CHANNEL_NAME
import com.example.autodialer.utils.constants.USER_LIST_EXTRA
import com.example.autodialer.utils.extensions.subscribeToCallEndCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class AutoCallService : LifecycleService() {
    private val TAG = "AutoCallService"

    private var users: ArrayList<User> = ArrayList()
    private var currentIndex = 0
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Toast.makeText(
                this@AutoCallService,
                "Stopping Service",
                Toast.LENGTH_SHORT
            ).show()
            stopSelf()
        } else {
            Log.e(TAG, "onStartCommand: started")
            createNotificationAndStartForeground()
            intent.getParcelableArrayListExtra<User>(USER_LIST_EXTRA).let {
                if (it != null) {
                    users.addAll(it)
                } else {
                    Toast.makeText(
                        this@AutoCallService,
                        "No Task.Stopping Service",
                        Toast.LENGTH_SHORT
                    ).show()
                    stopSelf()
                }
            }

            subscribeToCallStatesAndStartCall()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun subscribeToCallStatesAndStartCall() {
        subscribeToCallEndCallback {
            job?.cancel()
            job = lifecycleScope.launchWhenStarted {
                delay(2000)
                saveUserToDB(
                    users[currentIndex].copy(
                        durationInSec = getCallDurationFor(users[currentIndex].number).toLong()
                    )
                )

                if (currentIndex < users.size - 1)
                    startCall(users[++currentIndex].number)
                else {
                    Toast.makeText(
                        this@AutoCallService,
                        "Task complete.Stopping Service",
                        Toast.LENGTH_SHORT
                    ).show()
                    stopSelf()
                }
            }
        }
        startCall(users[currentIndex].number)
    }

    private fun createNotificationAndStartForeground() {
        createNotificationChannel()
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        else
            Notification.Builder(this)

        notificationBuilder
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentTitle("Auto dialer")
            .setContentText("In Progress")

        startForeground(1, notificationBuilder.build())
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startCall(number: String) {
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private suspend fun saveUserToDB(user: User) {
        AppDatabase.getInstance(this@AutoCallService).userDao().addOrUpdateUser(
            user
        )
    }

    private fun getCallDurationFor(number: String): String {
        val managedCursor: Cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null, null, null, CallLog.Calls.DATE + " DESC"
        ) ?: return ""

        val numberColumnIndex = managedCursor.getColumnIndex(CallLog.Calls.NUMBER)
        val durationColumnIndex = managedCursor.getColumnIndex(CallLog.Calls.DURATION)

        while (managedCursor.moveToNext()) {
            val phNumber = managedCursor.getString(numberColumnIndex).replace(" ","")
            if (phNumber == number.replace(" ","")) {
                val duration = managedCursor.getString(durationColumnIndex)
                managedCursor.close()
                return duration
            }
        }
        managedCursor.close()
        return "-999"
    }
}