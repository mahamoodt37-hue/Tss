package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ExtraCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtraCollectionDao {
    @Query("SELECT * FROM extra_collections ORDER BY timestamp DESC")
    fun getAllExtraCollections(): Flow<List<ExtraCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraCollection(extra: ExtraCollection): Long

    @Delete
    suspend fun deleteExtraCollection(extra: ExtraCollection)
}
