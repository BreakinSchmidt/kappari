package com.breakinschmidt.kappariwear.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.items
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.breakinschmidt.kappariwear.data.AuthManager
import com.breakinschmidt.kappariwear.data.GroceryRepository
import com.breakinschmidt.kappariwear.network.GroceryItem
import com.breakinschmidt.kappariwear.network.GroceryList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GroceryListScreen(token: String, authManager: AuthManager) {
    val navController = rememberSwipeDismissableNavController()
    val context = LocalContext.current
    val repository = remember { GroceryRepository(context) }
    
    val allLists by repository.getGroceryLists().collectAsState(initial = emptyList())
    val allGroceries by repository.getGroceries().collectAsState(initial = emptyList())
    
    var isLoading by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastListUid = prefs.getString("last_list_uid", null)
        val lastListTime = prefs.getLong("last_list_time", 0L)
        val currentTime = System.currentTimeMillis()
        
        // If it's been less than 1 hour (3600000 ms), go back to the last list
        if (lastListUid != null && (currentTime - lastListTime) < 3600000L) {
            navController.navigate("items/$lastListUid")
        }
    }

    LaunchedEffect(token) {
        try {
            repository.refreshGroceries(token)
            isOffline = false
        } catch (e: Exception) {
            e.printStackTrace()
            isOffline = true
        } finally {
            isLoading = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh when app comes to foreground
                isLoading = true
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        repository.refreshGroceries(token)
                        isOffline = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isOffline = true
                    } finally {
                        isLoading = false
                    }
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val currentEntry = navController.currentBackStackEntry
                if (currentEntry?.destination?.route == "items/{listUid}") {
                    val uid = currentEntry.arguments?.getString("listUid")
                    if (uid != null) {
                        prefs.edit()
                            .putString("last_list_uid", uid)
                            .putLong("last_list_time", System.currentTimeMillis())
                            .apply()
                    }
                } else {
                    prefs.edit()
                        .remove("last_list_uid")
                        .remove("last_list_time")
                        .apply()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "lists"
    ) {
        composable("lists") {
            ListSelectionScreen(
                lists = allLists,
                isLoading = isLoading,
                isOffline = isOffline,
                onListSelected = { listUid ->
                    navController.navigate("items/$listUid")
                }
            )
        }
        composable("items/{listUid}") { backStackEntry ->
            val listUid = backStackEntry.arguments?.getString("listUid")
            val listItems = allGroceries.filter { it.listUid == listUid && !it.purchased }.sortedBy { it.orderFlag }
            GroceryItemsScreen(
                groceries = listItems,
                isLoading = isLoading,
                isOffline = isOffline,
                repository = repository
            )
        }
    }
}

@Composable
fun ListSelectionScreen(
    lists: List<GroceryList>,
    isLoading: Boolean,
    isOffline: Boolean,
    onListSelected: (String) -> Unit
) {
    if (isLoading && lists.isEmpty()) {
        Text("Loading...", modifier = Modifier.fillMaxSize().padding(32.dp))
        return
    }

    if (lists.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOffline) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colors.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text("No lists found!")
        }
        return
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isOffline) {
            item(key = "offline_warning") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Offline",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Offline", color = MaterialTheme.colors.error, fontWeight = FontWeight.Bold)
                }
            }
        }
        item(key = "header_lists") {
            ListHeader {
                Text(
                    text = "Shopping Lists",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.secondary
                )
            }
        }
        items(lists, key = { it.uid }) { list ->
            Chip(
                onClick = { onListSelected(list.uid) },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(list.name) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun GroceryItemsScreen(
    groceries: List<GroceryItem>,
    isLoading: Boolean,
    isOffline: Boolean,
    repository: GroceryRepository
) {
    var checkedItemUids by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastPurchasedItem by remember { mutableStateOf<GroceryItem?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (isLoading && groceries.isEmpty()) {
        Text("Loading...", modifier = Modifier.fillMaxSize().padding(32.dp))
        return
    }

    if (groceries.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOffline) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colors.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text("No groceries!")
        }
        return
    }

    val groupedGroceries = groceries.groupBy { it.aisle ?: "Uncategorized" }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isOffline) {
            item(key = "offline_warning") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Offline",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Offline", color = MaterialTheme.colors.error, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (lastPurchasedItem != null) {
            item(key = "undo_${lastPurchasedItem!!.uid}") {
                Chip(
                    onClick = {
                        coroutineScope.launch {
                            repository.unmarkPurchased(lastPurchasedItem!!.uid)
                            lastPurchasedItem = null
                        }
                    },
                    colors = ChipDefaults.primaryChipColors(),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Undo"
                        )
                    },
                    label = { Text("Undo ${lastPurchasedItem!!.name}") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        groupedGroceries.forEach { (aisle, aisleItems) ->
            item(key = "header_$aisle") {
                ListHeader {
                    Text(
                        text = aisle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.secondary
                    )
                }
            }
            items(aisleItems, key = { it.uid }) { item ->
                val isChecked = checkedItemUids.contains(item.uid)
                AnimatedVisibility(
                    visible = !isChecked,
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ToggleChip(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            if (checked) {
                                checkedItemUids = checkedItemUids + item.uid
                                coroutineScope.launch {
                                    delay(400)
                                    try {
                                        repository.markPurchased(item.uid)
                                        lastPurchasedItem = item
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    checkedItemUids = checkedItemUids - item.uid
                                }
                            }
                        },
                        label = { Text(item.name) },
                        toggleControl = {
                            Checkbox(
                                checked = isChecked,
                                modifier = Modifier
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
