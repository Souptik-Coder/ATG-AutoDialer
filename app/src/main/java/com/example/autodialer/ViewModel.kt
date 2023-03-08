package com.example.autodialer

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.autodialer.db.AppDatabase
import com.example.autodialer.models.User
import com.example.autodialer.utils.constants.NAME_INDEX_IN_CSV
import com.example.autodialer.utils.constants.NUMBER_INDEX_IN_CSV
import com.example.autodialer.utils.constants.USER_ID_INDEX_IN_CSV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

class ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao by lazy { AppDatabase.getInstance(application).userDao() }

    init {
        getAllUsers()
    }

    private val _allUsers: MutableStateFlow<List<User>> = MutableStateFlow(ArrayList())
    val allUsers = _allUsers.asStateFlow()

    private val _isFileValid: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val isFileValid = _isFileValid.asStateFlow()

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private fun addOrUpdateUser(user: User) =
        viewModelScope.launch {
            dao.addOrUpdateUser(user)
        }

    private fun getAllUsers() = viewModelScope.launch {
        dao.getAllUsers().collectLatest { _allUsers.value = it }
    }

    fun exportUserDataTo(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val fos = getApplication<Application>().contentResolver.openOutputStream(uri)
        fos?.write(getUserDataInCSVString().toByteArray())
        fos?.flush()
        fos?.close()
    }

    private fun getUserDataInCSVString(): String {
        return buildString {
            _allUsers.value.forEach { user ->
                append("\"${user.userId}\",")
                append("\"${user.number}\",")
                append("\"${user.name}\",")
                append("${user.additionalInfo},")
                append("\"${user.durationInSec}\"")
                append("\n")
            }
        }
    }

    fun readCSVFromURI(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true
        removeAllUsers()
        val reader = BufferedReader(
            InputStreamReader(
                getApplication<Application>().contentResolver.openInputStream(uri),
                Charset.forName("UTF-8")
            )
        )
        try {
            reader.readLines().forEach { line ->
                val items = line.split(",")
                addOrUpdateUser(
                    User(
                        userId = items[USER_ID_INDEX_IN_CSV].replace("\"", ""),
                        name = items[NAME_INDEX_IN_CSV].replace("\"", ""),
                        number = items[NUMBER_INDEX_IN_CSV].filter { it.isLetterOrDigit() || it == '+' },
                        additionalInfo = items.subList(NAME_INDEX_IN_CSV + 1, items.size)
                            .joinToString(",")
                    )
                )
            }
            _isFileValid.value = true
        } catch (e: Exception) {
            _isFileValid.value = false
        } finally {
            _isLoading.value = false
        }
    }

    fun removeAllUsers() = viewModelScope.launch {
        dao.removeAll()
    }
}