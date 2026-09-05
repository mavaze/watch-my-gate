package com.mavaze.mygate.ui.watchman

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mavaze.mygate.data.local.User

@Composable
fun WatchmanHomeScreen(user: User) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome, ${user.displayName.ifBlank { "Watchman" }}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Use the navigation below to manage the gate.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Call, contentDescription = null)
            Icon(Icons.Default.Person, contentDescription = null)
            Icon(Icons.Default.Task, contentDescription = null)
        }
    }
}
