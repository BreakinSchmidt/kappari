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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
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
            item {
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
        item {
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
            item {
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
        groupedGroceries.forEach { (aisle, aisleItems) ->
            item {
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
