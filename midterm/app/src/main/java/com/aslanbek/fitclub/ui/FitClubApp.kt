package com.aslanbek.fitclub.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aslanbek.fitclub.ui.addworkout.AddWorkoutRoute
import com.aslanbek.fitclub.ui.bookings.BookingsRoute
import com.aslanbek.fitclub.ui.details.WorkoutDetailsRoute
import com.aslanbek.fitclub.ui.navigation.Routes
import com.aslanbek.fitclub.ui.navigation.TopLevelDestination
import com.aslanbek.fitclub.ui.progress.ProgressRoute
import com.aslanbek.fitclub.ui.schedule.ScheduleRoute
import com.aslanbek.fitclub.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FitClubApp(appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val upcomingCount by appViewModel.upcomingCount.collectAsStateWithLifecycle()

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }
    ObserveAsEvents(appViewModel.notifications, showMessage)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { currentDestination.isOn(it.route) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                FitClubBottomBar(
                    currentDestination = currentDestination,
                    upcomingCount = upcomingCount,
                    onNavigate = { destination -> navController.navigateToTopLevel(destination) }
                )
            }
        }
    ) { innerPadding ->
        FitClubNavHost(
            navController = navController,
            showMessage = showMessage,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}

@Composable
private fun FitClubNavHost(
    navController: NavHostController,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.SCHEDULE.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.SCHEDULE.route) {
            ScheduleRoute(
                onOpenWorkout = { id -> navController.navigate(Routes.workoutDetails(id)) },
                onAddWorkout = { navController.navigate(Routes.ADD_WORKOUT) }
            )
        }
        composable(TopLevelDestination.BOOKINGS.route) {
            BookingsRoute(
                onOpenWorkout = { id -> navController.navigate(Routes.workoutDetails(id)) },
                onMessage = showMessage
            )
        }
        composable(TopLevelDestination.PROGRESS.route) {
            ProgressRoute()
        }
        composable(
            route = Routes.WORKOUT_DETAILS,
            arguments = listOf(navArgument(Routes.WORKOUT_ID) { type = NavType.IntType })
        ) {
            WorkoutDetailsRoute(
                onBack = { navController.popBackStack() },
                onMessage = showMessage
            )
        }
        composable(Routes.ADD_WORKOUT) {
            AddWorkoutRoute(
                onBack = { navController.popBackStack() },
                onSaved = { message ->
                    navController.popBackStack()
                    showMessage(message)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FitClubBottomBar(
    currentDestination: NavDestination?,
    upcomingCount: Int,
    onNavigate: (TopLevelDestination) -> Unit
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination.isOn(destination.route),
                onClick = { onNavigate(destination) },
                icon = {
                    if (destination == TopLevelDestination.BOOKINGS && upcomingCount > 0) {
                        BadgedBox(badge = { Badge { Text(upcomingCount.toString()) } }) {
                            Icon(destination.icon, contentDescription = destination.label)
                        }
                    } else {
                        Icon(destination.icon, contentDescription = destination.label)
                    }
                },
                label = { Text(destination.label) }
            )
        }
    }
}

private fun NavDestination?.isOn(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
