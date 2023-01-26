package com.example.autodialer.ui

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.autodialer.adapters.ResultAdapter
import com.example.autodialer.databinding.ActivityResultBinding
import com.example.autodialer.db.AppDatabase
import com.example.autodialer.services.AutoCallService
import com.example.autodialer.utils.constants.USER_LIST_EXTRA
import kotlinx.coroutines.flow.collectLatest

class ResultActivity : AppCompatActivity() {

    private val resultAdapter: ResultAdapter by lazy { ResultAdapter() }
    private lateinit var binding: ActivityResultBinding
    private val serviceIntent by lazy {
        Intent(this@ResultActivity, AutoCallService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)

        setContentView(binding.root)

        lifecycleScope.launchWhenStarted {
            AppDatabase.getInstance(this@ResultActivity).userDao().getAllUsers().collectLatest {
                resultAdapter.submitList(it)
                serviceIntent.putParcelableArrayListExtra(USER_LIST_EXTRA, it as ArrayList)
            }

        }
        binding.btnCall.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            }else
                startService(serviceIntent)
        }

        binding.recyclerView.adapter = resultAdapter
    }
}