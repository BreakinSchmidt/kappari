package com.breakinschmidt.kappariwear.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.breakinschmidt.kappariwear.data.local.AppDatabase
import com.breakinschmidt.kappariwear.data.local.SyncStatus
import com.breakinschmidt.kappariwear.data.local.toEntity
import com.breakinschmidt.kappariwear.data.local.toNetworkModel
import com.breakinschmidt.kappariwear.network.GroceryItem
import com.breakinschmidt.kappariwear.network.GroceryList
import com.breakinschmidt.kappariwear.network.PaprikaApiClient
import com.breakinschmidt.kappariwear.worker.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroceryRepository(private val context: Context) {
    private val groceryDao = AppDatabase.getDatabase(context).groceryDao()

    fun getGroceries(): Flow<List<GroceryItem>> {
        return groceryDao.getGroceries().map { entities ->
            entities.map { it.toNetworkModel() }
        }
    }

    fun getGroceryLists(): Flow<List<GroceryList>> {
        return groceryDao.getGroceryLists().map { entities ->
            entities.map { it.toNetworkModel() }
        }
    }

    suspend fun refreshGroceries(token: String) {
        // Fetch both lists and items
        val listResponse = PaprikaApiClient.api.getGroceryLists("Bearer $token")
        val itemResponse = PaprikaApiClient.api.getGroceries("Bearer $token")
        
        // Update lists
        groceryDao.clearAllLists()
        groceryDao.insertAllLists(listResponse.result.map { it.toEntity() })

        // Update items
        val pendingUpdates = groceryDao.getPendingUpdates().associateBy { it.uid }
        val newEntities = itemResponse.result.map { item ->
            pendingUpdates[item.uid] ?: item.toEntity()
        }
        
        groceryDao.deleteSynced()
        groceryDao.insertAll(newEntities)
    }

    suspend fun markPurchased(uid: String) {
        val entity = groceryDao.getGroceryById(uid)
        if (entity != null) {
            val updated = entity.copy(purchased = true, syncStatus = SyncStatus.PENDING_UPDATE)
            groceryDao.update(updated)
            enqueueSyncWorker()
        }
    }

    suspend fun unmarkPurchased(uid: String) {
        val entity = groceryDao.getGroceryById(uid)
        if (entity != null) {
            val updated = entity.copy(purchased = false, syncStatus = SyncStatus.PENDING_UPDATE)
            groceryDao.update(updated)
            enqueueSyncWorker()
        }
    }
    
    fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncGroceriesWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
