package com.example.autodialer.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.autodialer.R
import com.example.autodialer.db.AppDatabase
import com.example.autodialer.models.User
import com.google.android.material.button.MaterialButton
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private val csvPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null)
                Toast.makeText(this, "No file selected.Please try again", Toast.LENGTH_LONG).show()
            else
                readCSVFromURI(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAppPermissionsIfNeeded()

        findViewById<MaterialButton>(R.id.upload_btn).setOnClickListener {
            csvPickerLauncher.launch("text/*")
        }
    }

    private fun readCSVFromURI(uri: Uri) {
        val reader = BufferedReader(
            InputStreamReader(
                contentResolver.openInputStream(uri),
                Charset.forName("UTF-8")
            )
        )
        try {
            reader.readLines().forEach {
                val items = it.split(",")
                saveUserToDB(User(items[0], items[1].toLong()))
            }
            startResultActivity()
            finish()
        }catch (e: Exception) {
                Toast.makeText(this, "Invalid CSV file ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    private fun saveUserToDB(user: User) {
        lifecycleScope.launchWhenStarted {
            AppDatabase.getInstance(this@MainActivity).userDao().addOrUpdateUser(
                user
            )
        }
    }

    private fun requestAppPermissionsIfNeeded() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                "android.permission.CALL_PHONE"
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.CALL_PHONE"), 2
            )

        }

        if (ContextCompat.checkSelfPermission(
                this,
                "android.permission.READ_CALL_LOG"
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.READ_CALL_LOG"), 3
            )

        }
    }

    private fun startResultActivity() {
        startActivity(Intent(this, ResultActivity::class.java))
    }
}

