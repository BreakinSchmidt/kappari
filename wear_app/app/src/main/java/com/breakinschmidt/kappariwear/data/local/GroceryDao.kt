package com.breakinschmidt.kappariwear.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {
    @Query("SELECT * FROM groceries ORDER BY orderFlag ASC")
    fun getGroceries(): Flow<List<GroceryEntity>>

    @Query("SELECT * FROM groceries WHERE syncStatus = 'PENDING_UPDATE'")
    suspend fun getPendingUpdates(): List<GroceryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groceries: List<GroceryEntity>)

    @Update
    suspend fun update(grocery: GroceryEntity)
    
    @Query("DELETE FROM groceries")
    suspend fun clearAll()
    
    @Query("DELETE FROM groceries WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSynced()
    
    @Query("SELECT * FROM groceries WHERE uid = :uid")
    suspend fun getGroceryById(uid: String): GroceryEntity?
    @Query("SELECT * FROM grocery_lists ORDER BY orderFlag ASC")
    fun getGroceryLists(): Flow<List<GroceryListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLists(lists: List<GroceryListEntity>)

    @Query("DELETE FROM grocery_lists")
    suspend fun clearAllLists()
}
