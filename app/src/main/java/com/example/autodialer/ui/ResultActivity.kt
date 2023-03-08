package com.example.autodialer.ui

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.autodialer.ViewModel
import com.example.autodialer.adapters.ResultAdapter
import com.example.autodialer.databinding.ActivityResultBinding
import com.example.autodialer.db.AppDatabase
import com.example.autodialer.services.AutoCallService
import com.example.autodialer.utils.constants.USER_LIST_EXTRA
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ResultActivity : AppCompatActivity() {

    private val viewModel: ViewModel by viewModels()

    private val resultAdapter: ResultAdapter by lazy { ResultAdapter() }
    private lateinit var binding: ActivityResultBinding
    private val serviceIntent by lazy {
        Intent(this@ResultActivity, AutoCallService::class.java)
    }

    private val exportFilePicker =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri == null)
                Toast.makeText(this, "No directory selected.Please try again", Toast.LENGTH_LONG)
                    .show()
            else
                viewModel.exportUserDataTo(uri)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)

        setContentView(binding.root)


        lifecycleScope.launchWhenStarted {
            viewModel.allUsers.collectLatest {
                resultAdapter.submitList(it)
                serviceIntent.putParcelableArrayListExtra(USER_LIST_EXTRA, it as ArrayList)
            }
        }


        binding.btnCall.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else
                startService(serviceIntent)
        }

        binding.exportBtn.setOnClickListener {
            exportFilePicker.launch(
                "AutoDialer_Export_" + SimpleDateFormat(
                    "yyyy/MM/dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())
            )
        }

        binding.restartBtn.setOnClickListener {
            val builder = AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Do you want to go back and restart? This will clear current data")
                .setPositiveButton(
                    "Restart"
                ) { _, _ ->
                    viewModel.removeAllUsers()
                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        )
                    );finish()
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog, _ -> dialog.dismiss() }

            builder.create().show()
        }

        binding.recyclerView.adapter = resultAdapter
    }
}