package com.mavaze.mygate.ui.watchman

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mavaze.mygate.data.local.User

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WatchmanNavHost(
    user: User,
    state: WatchmanUiState,
    onCallAlias: (WatchmanAlias) -> Unit,
    onMockCallAlias: (WatchmanAlias) -> Unit,
    onCancelCall: () -> Unit,
    onSkipCall: () -> Unit,
    onRequestDialerRole: () -> Unit,
    dialerRoleHeld: Boolean,
    callPermissionGranted: Boolean,
    onRequestCallPermission: () -> Unit,
    callLogPermissionGranted: Boolean,
    onRequestCallLogPermission: () -> Unit,
    onRefreshCallHistory: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var profileExpanded by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: WatchmanRoutes.HOME
    val topLevel = route in setOf(
        WatchmanRoutes.HOME,
        WatchmanRoutes.RESIDENT_MEMBERS,
        WatchmanRoutes.VISITORS,
        WatchmanRoutes.TASKS,
        WatchmanRoutes.HISTORY
    )
    val showBottomBar = topLevel
    val title = when (route) {
        WatchmanRoutes.HOME -> "My Gate"
        WatchmanRoutes.RESIDENT_MEMBERS -> "Residents"
        WatchmanRoutes.VISITORS -> "Visitors"
        WatchmanRoutes.TASKS -> "Tasks"
        WatchmanRoutes.HISTORY -> "History"
        WatchmanRoutes.CALL_HISTORY -> "Call History"
        WatchmanRoutes.VISITOR_HISTORY -> "Visitor History"
        WatchmanRoutes.ADD_VISITOR -> "Add Visitor"
        WatchmanRoutes.VISITOR_DETAILS -> "Visitor Details"
        WatchmanRoutes.UPDATE_VISITOR_PHOTO -> "Update Visitor Photo"
        WatchmanRoutes.CALL_SEQUENCE -> "Call Sequence"
        else -> "My Gate"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (topLevel) {
                TopAppBar(
                    title = { Text(title) },
                    actions = {
                        IconButton(onClick = { navController.navigate(WatchmanRoutes.NOTIFICATIONS) }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                        Box {
                            IconButton(onClick = { profileExpanded = true }) {
                                WatchmanAvatar(user.displayName)
                            }
                            DropdownMenu(
                                expanded = profileExpanded,
                                onDismissRequest = { profileExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(user.displayName.ifBlank { "Watchman" })
                                    },
                                    leadingIcon = { WatchmanAvatar(user.displayName) },
                                    onClick = {
                                        profileExpanded = false
                                        navController.navigate(WatchmanRoutes.PROFILE)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Logout") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Logout, contentDescription = null)
                                    },
                                    onClick = {
                                        profileExpanded = false
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (route == WatchmanRoutes.CALL_SEQUENCE && state.activeCall != null) {
                                onCancelCall()
                            }
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    WatchmanBottomItem(navController, route, WatchmanRoutes.HOME, "Home", Icons.Default.Home)
                    WatchmanBottomItem(navController, route, WatchmanRoutes.RESIDENT_MEMBERS, "Residents", Icons.Default.People)
                    WatchmanBottomItem(navController, route, WatchmanRoutes.VISITORS, "Visitors", Icons.Default.Person)
                    WatchmanBottomItem(navController, route, WatchmanRoutes.TASKS, "Tasks", Icons.Default.Task)
                    WatchmanBottomItem(navController, route, WatchmanRoutes.HISTORY, "History", Icons.Default.History)
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = WatchmanRoutes.HOME,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(WatchmanRoutes.HOME) {
                WatchmanHomeScreen(user = user)
            }
            composable(WatchmanRoutes.RESIDENT_MEMBERS) {
                ResidentMembersScreen(state = state, onCallAlias = { alias ->
                    onCallAlias(alias)
                    navController.navigate(WatchmanRoutes.CALL_SEQUENCE)
                })
            }
            composable(WatchmanRoutes.CALL_SEQUENCE) {
                CallSequenceScreen(
                    user = user,
                    state = state,
                    onCallAlias = onCallAlias,
                    onMockCallAlias = onMockCallAlias,
                    onCancelCall = onCancelCall,
                    onSkipCall = onSkipCall,
                    onRequestDialerRole = onRequestDialerRole,
                    dialerRoleHeld = dialerRoleHeld,
                    callPermissionGranted = callPermissionGranted,
                    onRequestCallPermission = onRequestCallPermission
                )
            }
            composable(WatchmanRoutes.VISITORS) {
                VisitorsScreen(
                    onAddVisitor = { navController.navigate(WatchmanRoutes.ADD_VISITOR) },
                    onVisitorDetails = { id -> navController.navigate(WatchmanRoutes.visitorDetails(id)) }
                )
            }
            composable(WatchmanRoutes.ADD_VISITOR) {
                AddVisitorScreen(onSave = { navController.popBackStack() })
            }
            composable(WatchmanRoutes.VISITOR_DETAILS) { entry ->
                VisitorDetailsScreen(
                    visitorId = entry.arguments?.getString("visitorId") ?: "",
                    onUpdatePhoto = { id -> navController.navigate(WatchmanRoutes.updateVisitorPhoto(id)) }
                )
            }
            composable(WatchmanRoutes.UPDATE_VISITOR_PHOTO) { entry ->
                UpdateVisitorPhotoScreen(visitorId = entry.arguments?.getString("visitorId") ?: "")
            }
            composable(WatchmanRoutes.TASKS) { TasksScreen(onAddTask = { navController.navigate(WatchmanRoutes.ADD_TASK) }) }
            composable(WatchmanRoutes.ADD_TASK) { AddTaskScreen(onSave = { navController.popBackStack() }) }
            composable(WatchmanRoutes.HISTORY) {
                HistoryScreen(
                    onCallHistory = { navController.navigate(WatchmanRoutes.CALL_HISTORY) },
                    onVisitorHistory = { navController.navigate(WatchmanRoutes.VISITOR_HISTORY) }
                )
            }
            composable(WatchmanRoutes.CALL_HISTORY) {
                CallHistoryScreen(
                    entries = state.nativeCallHistory,
                    permissionGranted = callLogPermissionGranted,
                    onRequestPermission = onRequestCallLogPermission,
                    onRefresh = onRefreshCallHistory
                )
            }
            composable(WatchmanRoutes.VISITOR_HISTORY) { VisitorHistoryScreen() }
            composable(WatchmanRoutes.NOTIFICATIONS) { NotificationsScreen() }
            composable(WatchmanRoutes.PROFILE) { ProfileScreen(user = user, onLogout = onLogout) }
        }
    }
}

@Composable
private fun RowScope.WatchmanBottomItem(
    navController: NavHostController,
    currentRoute: String,
    route: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    NavigationBarItem(
        selected = currentRoute == route,
        onClick = {
            navController.navigate(route) {
                popUpTo(WatchmanRoutes.HOME) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        },
        label = {
            Text(label)
        },
        alwaysShowLabel = true
    )
}

@Composable
private fun WatchmanAvatar(name: String) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
    Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 2.dp) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) { Text(initial, style = MaterialTheme.typography.titleMedium) }
    }
}
