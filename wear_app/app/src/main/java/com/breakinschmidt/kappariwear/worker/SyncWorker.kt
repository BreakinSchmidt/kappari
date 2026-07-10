package com.breakinschmidt.kappariwear.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.breakinschmidt.kappariwear.data.AuthManager
import com.breakinschmidt.kappariwear.data.GroceryRepository
import com.breakinschmidt.kappariwear.data.local.AppDatabase
import com.breakinschmidt.kappariwear.data.local.SyncStatus
import com.breakinschmidt.kappariwear.data.local.toNetworkModel
import com.breakinschmidt.kappariwear.network.PaprikaApiClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val groceryDao = database.groceryDao()
        val authManager = AuthManager(applicationContext)
        val token = authManager.jwtToken.firstOrNull() ?: return Result.failure()
        
        val pendingUpdates = groceryDao.getPendingUpdates()
        if (pendingUpdates.isNotEmpty()) {
            val gson = Gson()
            
            for (entity in pendingUpdates) {
                try {
                    val item = entity.toNetworkModel()
                    val jsonString = gson.toJson(listOf(item))
                    
                    val baos = ByteArrayOutputStream()
                    GZIPOutputStream(baos).use { it.write(jsonString.toByteArray()) }
                    val gzipped = baos.toByteArray()
                    
                    val requestBody = gzipped.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("data", "file", requestBody)
                    
                    PaprikaApiClient.api.syncGroceries("Bearer $token", part)
                    
                    // Mark as synced if successful
                    groceryDao.update(entity.copy(syncStatus = SyncStatus.SYNCED))
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Retry if network fails or server error
                    return Result.retry()
                }
            }
        }

        // Pull latest updates
        try {
            val repository = GroceryRepository(applicationContext)
            repository.refreshGroceries(token)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
        
        return Result.success()
    }
}
