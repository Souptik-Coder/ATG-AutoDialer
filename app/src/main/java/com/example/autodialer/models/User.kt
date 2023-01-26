package com.example.autodialer.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@kotlinx.parcelize.Parcelize
@Entity(tableName = "users")
data class User(
    val name: String="",
    @PrimaryKey(autoGenerate = false) val number: Long,
    val durationInMs: Long = -1
):Parcelable
