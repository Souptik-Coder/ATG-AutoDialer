package com.example.autodialer.ui

import android.Manifest.permission.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.autodialer.ViewModel
import com.example.autodialer.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {

    private val viewModel: ViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private val requestSinglePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            it.forEach { (_, isGranted) ->
                if (!isGranted) {
                    Toast.makeText(
                        this,
                        "All permissions are required for app to work",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }

    private val csvPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null)
                Toast.makeText(this, "No file selected.Please try again", Toast.LENGTH_LONG).show()
            else
                viewModel.readCSVFromURI(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        requestAppPermissionsIfNeeded()

        binding.uploadBtn.setOnClickListener {
            csvPickerLauncher.launch("*/*")
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isFileValid.collectLatest {
                if (it == null) return@collectLatest
                else if (it) {
                    startResultActivity()
                    binding.fileInvalidLayout.visibility = View.GONE
                } else binding.fileInvalidLayout.visibility = View.VISIBLE
            }

        }
        lifecycleScope.launchWhenStarted {
            viewModel.allUsers.collectLatest { if (it.isNotEmpty()) startResultActivity() }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isFileValid.collectLatest {
                binding.progressBar.isVisible = it ?: false
            }
        }
    }


    private fun requestAppPermissionsIfNeeded() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow AutoDialer to draw over other apps", Toast.LENGTH_LONG)
                .show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                READ_CALL_LOG
            )
            != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestSinglePermissionLauncher.launch(
                arrayOf(
                    READ_CALL_LOG,
                    CALL_PHONE,
                    READ_PHONE_STATE
                )
            )
        }
    }

    private fun startResultActivity() {
        startActivity(Intent(this, ResultActivity::class.java))
    }
}

