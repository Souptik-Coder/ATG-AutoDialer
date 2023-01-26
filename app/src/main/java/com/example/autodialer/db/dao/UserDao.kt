package com.example.autodialer.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.autodialer.models.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
 /*   suspend fun insertAllUser()

    @Query("SELECT * FROM user WHERE duration!=-1")
    suspend fun getUnCalledUsers(): Flow<User>*/

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     suspend fun addOrUpdateUser(user:User)
}
