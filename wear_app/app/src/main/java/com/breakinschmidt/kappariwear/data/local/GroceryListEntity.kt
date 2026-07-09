package com.breakinschmidt.kappariwear.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.breakinschmidt.kappariwear.network.GroceryList

@Entity(tableName = "grocery_lists")
data class GroceryListEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val orderFlag: Int,
    val isDefault: Boolean
)

fun GroceryListEntity.toNetworkModel() = GroceryList(
    uid = uid,
    name = name,
    orderFlag = orderFlag,
    isDefault = isDefault
)

fun GroceryList.toEntity() = GroceryListEntity(
    uid = uid,
    name = name,
    orderFlag = orderFlag,
    isDefault = isDefault
)
