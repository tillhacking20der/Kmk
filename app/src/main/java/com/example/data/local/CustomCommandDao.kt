package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CustomCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCommandDao {
    @Query("SELECT * FROM custom_commands ORDER BY createdAt DESC")
    fun getAllCommands(): Flow<List<CustomCommand>>

    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabledCommands(): Flow<List<CustomCommand>>

    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1")
    suspend fun getEnabledCommandsList(): List<CustomCommand>

    @Query("SELECT * FROM custom_commands WHERE id = :id LIMIT 1")
    suspend fun getCommandById(id: Long): CustomCommand?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: CustomCommand): Long

    @Update
    suspend fun update(command: CustomCommand)

    @Delete
    suspend fun delete(command: CustomCommand)

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM custom_commands")
    suspend fun getCount(): Int
}
