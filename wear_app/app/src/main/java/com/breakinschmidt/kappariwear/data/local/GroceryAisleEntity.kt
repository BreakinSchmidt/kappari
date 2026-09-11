package com.breakinschmidt.kappariwear.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.breakinschmidt.kappariwear.network.GroceryAisle

@Entity(tableName = "grocery_aisles")
data class GroceryAisleEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val orderFlag: Int
)

fun GroceryAisleEntity.toNetworkModel() = GroceryAisle(
    uid = uid,
    name = name,
    orderFlag = orderFlag
)

fun GroceryAisle.toEntity() = GroceryAisleEntity(
    uid = uid,
    name = name,
    orderFlag = orderFlag
)
