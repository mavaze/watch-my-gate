package com.mavaze.mygate.ui.watchman

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mavaze.mygate.data.local.User
import java.util.Date

@Composable
fun VisitorsScreen(onAddVisitor: () -> Unit, onVisitorDetails: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Visitor management will be implemented next.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddVisitor, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Add Visitor")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { onVisitorDetails("placeholder") }, modifier = Modifier.fillMaxWidth()) {
            Text("Open Visitor Details (placeholder)")
        }
    }
}

@Composable fun AddVisitorScreen(onSave: () -> Unit) { PlaceholderSecondary("Add Visitor", "Visitor entry UI will be implemented next.", onSave, "Save Visitor") }

@Composable
fun VisitorDetailsScreen(visitorId: String, onUpdatePhoto: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Visitor ID: $visitorId", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp)); Text("Visitor details will be implemented next.")
        Spacer(Modifier.height(24.dp)); Button(onClick = { onUpdatePhoto(visitorId) }) { Text("Update Photo") }
    }
}

@Composable fun UpdateVisitorPhotoScreen(visitorId: String) { PlaceholderContent("Update Visitor Photo", "Photo UI placeholder for visitor $visitorId.") }

@Composable
fun TasksScreen(onAddTask: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Today", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        TaskCard("Water tank check", "Today · 10:30 AM · Daily")
        Spacer(Modifier.height(8.dp)); TaskCard("Security round", "Today · 2:00 PM · Daily")
        Spacer(Modifier.height(20.dp)); Text("Upcoming", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp)); TaskCard("Garbage collection", "Tomorrow · 7:00 AM · Weekly")
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onAddTask, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Add Task")
        }
    }
}

@Composable private fun TaskCard(title: String, subtitle: String) {
    ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp)) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle) }; IconButton(onClick = {}) { Icon(Icons.Default.Check, "Done") } } }
}

@Composable fun AddTaskScreen(onSave: () -> Unit) { PlaceholderSecondary("New Task", "Task creation will be connected to Google Calendar later.", onSave, "Save") }

@Composable
fun HistoryScreen(onCallHistory: () -> Unit, onVisitorHistory: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onCallHistory, modifier = Modifier.fillMaxWidth()) { Text("Call History") }
        Spacer(Modifier.height(12.dp)); Button(onClick = onVisitorHistory, modifier = Modifier.fillMaxWidth()) { Text("Visitor History") }
    }
}

@Composable
fun CallHistoryScreen(entries: List<NativeCallHistoryEntry>, permissionGranted: Boolean, onRequestPermission: () -> Unit, onRefresh: () -> Unit) {
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) onRefresh()
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Latest calls", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }
        if (!permissionGranted) {
            Spacer(Modifier.height(16.dp)); Text("Call history permission is required to read the phone's native call log.")
            Spacer(Modifier.height(12.dp)); Button(onClick = onRequestPermission) { Text("Allow call history") }
        } else if (entries.isEmpty()) {
            Spacer(Modifier.height(24.dp)); Text("No resident calls found.")
        } else {
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp)) {
                            Icon(if (entry.incoming) Icons.Default.CallReceived else Icons.Default.CallMade, contentDescription = if (entry.incoming) "Incoming" else "Outgoing")
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.contactName, style = MaterialTheme.typography.titleMedium)
                                Text(entry.alias)
                                Text(entry.details)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun VisitorHistoryScreen() = PlaceholderContent("Visitor History", "Visitor history will be implemented with Visitors.")
@Composable fun NotificationsScreen() = PlaceholderContent("Notifications", "No new notifications.")

@Composable
fun ProfileScreen(user: User, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(user.displayName.ifBlank { "Watchman" }, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp)); Text("Watchman")
        Spacer(Modifier.height(24.dp)); OutlinedButton(onClick = onLogout) { Text("Logout") }
    }
}

@Composable private fun PlaceholderSecondary(title: String, text: String, onAction: () -> Unit, action: String) {
    Column(Modifier.fillMaxSize().padding(24.dp)) { Text(text); Spacer(Modifier.height(24.dp)); Button(onClick = onAction) { Text(action) } }
}
@Composable private fun PlaceholderContent(title: String, text: String) { Column(Modifier.fillMaxSize().padding(24.dp)) { Text(title, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(16.dp)); Text(text) } }
