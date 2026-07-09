package com.breakinschmidt.kappariwear.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val result: LoginResult? = null,
    val error: PaprikaError? = null
)

data class PaprikaError(
    val code: Int? = null,
    val message: String? = null
)

data class LoginResult(
    val token: String
)

data class GroceryResponse(
    val result: List<GroceryItem> = emptyList()
)

data class GroceryItem(
    val uid: String,
    val name: String,
    val aisle: String? = null,
    var purchased: Boolean = false,
    @SerializedName("order_flag") val orderFlag: Int = 0,
    val recipe: String? = null,
    @SerializedName("list_uid") val listUid: String? = null
)

data class GroceryListResponse(
    val result: List<GroceryList> = emptyList()
)

data class GroceryList(
    val uid: String,
    val name: String,
    @SerializedName("order_flag") val orderFlag: Int = 0,
    @SerializedName("is_default") val isDefault: Boolean = false
)
