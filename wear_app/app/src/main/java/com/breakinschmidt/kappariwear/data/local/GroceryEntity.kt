package com.breakinschmidt.kappariwear.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.breakinschmidt.kappariwear.network.GroceryItem

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING_UPDATE = "PENDING_UPDATE"
}

@Entity(tableName = "groceries")
data class GroceryEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val aisle: String?,
    val purchased: Boolean,
    val orderFlag: Int,
    val recipe: String?,
    val listUid: String?,
    val syncStatus: String = SyncStatus.SYNCED
)

fun GroceryEntity.toNetworkModel() = GroceryItem(
    uid = uid,
    name = name,
    aisle = aisle,
    purchased = purchased,
    orderFlag = orderFlag,
    recipe = recipe,
    listUid = listUid
)

fun GroceryItem.toEntity(syncStatus: String = SyncStatus.SYNCED) = GroceryEntity(
    uid = uid,
    name = name,
    aisle = aisle,
    purchased = purchased,
    orderFlag = orderFlag,
    recipe = recipe,
    listUid = listUid,
    syncStatus = syncStatus
)
