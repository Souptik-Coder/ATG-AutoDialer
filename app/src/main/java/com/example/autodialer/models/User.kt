package com.example.autodialer.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@kotlinx.parcelize.Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String = "",
    val number: String,
    val additionalInfo: String,
    val durationInSec: Long = -1
) : Parcelable
