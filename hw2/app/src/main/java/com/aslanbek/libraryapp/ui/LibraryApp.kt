package com.aslanbek.libraryapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aslanbek.libraryapp.ui.components.AddItemDialog
import com.aslanbek.libraryapp.ui.screens.CatalogScreen
import com.aslanbek.libraryapp.ui.screens.LoansScreen
import com.aslanbek.libraryapp.ui.screens.StateDemoScreen

enum class AppTab(val label: String, val icon: ImageVector) {
    CATALOG("Catalog", Icons.AutoMirrored.Filled.List),
    LOANS("My loans", Icons.Filled.Person),
    STATE("State demo", Icons.Filled.Info)
}

/**
 * Root composable. This is the only place that touches the ViewModel:
 * it reads uiState and passes plain values + event lambdas down to stateless screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryApp(viewModel: LibraryViewModel = viewModel()) {
    val state = viewModel.uiState

    // UI-only state that must survive rotation -> rememberSaveable
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.CATALOG) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    // Cheap object that can simply be created again -> remember
    val snackbarHostState = remember { SnackbarHostState() }

    // Show each message from the ViewModel once, then tell it the message was consumed
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message.text)
        viewModel.onMessageShown(message.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.libraryName)
                        Text(
                            text = "Reader: ${state.currentMember?.name ?: "—"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.isLoadingRatings) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = viewModel::loadRatings) {
                            Icon(Icons.Filled.Star, contentDescription = "Load ratings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == AppTab.CATALOG) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add item") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (selectedTab) {
            AppTab.CATALOG -> CatalogScreen(
                state = state,
                onQueryChange = viewModel::onQueryChange,
                onTypeFilterChange = viewModel::onTypeFilterChange,
                onOnlyAvailableChange = viewModel::onOnlyAvailableChange,
                onMemberSelected = viewModel::onMemberSelected,
                onBorrow = viewModel::borrow,
                onReturn = viewModel::returnItem,
                modifier = contentModifier
            )
            AppTab.LOANS -> LoansScreen(
                state = state,
                onMemberSelected = viewModel::onMemberSelected,
                onReturn = viewModel::returnItem,
                modifier = contentModifier
            )
            AppTab.STATE -> StateDemoScreen(
                viewModelCounter = state.viewModelCounter,
                viewModelId = viewModel.instanceId,
                onIncrementViewModelCounter = viewModel::incrementCounter,
                modifier = contentModifier
            )
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { input ->
                val error = viewModel.addItem(input)
                if (error == null) showAddDialog = false
                error
            }
        )
    }
}
