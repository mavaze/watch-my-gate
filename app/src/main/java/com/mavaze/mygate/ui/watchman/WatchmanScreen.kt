package com.mavaze.mygate.ui.watchman

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mavaze.mygate.data.local.User

@Composable
fun WatchmanScreen(
    user: User,
    state: WatchmanUiState,
    onCallAlias: (String) -> Unit,
    onMockCallAlias: (String) -> Unit,
    onCancelCall: () -> Unit,
    onSkipCall: () -> Unit,
    onRequestDialerRole: () -> Unit,
    dialerRoleHeld: Boolean,
    callPermissionGranted: Boolean,
    onRequestCallPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("My Gate", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Watchman", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))

        if (!dialerRoleHeld) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Phone calling needs My Gate to be the default phone app.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Enable this when actual phone calling is ready.")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRequestDialerRole,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Enable phone calling") }
                }
            }
        } else if (!callPermissionGranted) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "My Gate needs phone permission to place and manage calls.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRequestCallPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Allow phone calls") }
                }
            }
        }

        if (state.activeCall != null) {
            val call = state.activeCall
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Calling ${call.attemptNumber}. ${call.contactName}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text("(${call.alias})", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Person ${call.attemptNumber} of ${call.totalContacts}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(28.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelCall,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = onSkipCall,
                        modifier = Modifier.weight(1f)
                    ) { Text("Skip") }
                }
            }
        } else {
            Spacer(Modifier.height(24.dp))
            Text("Residents", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.aliases, key = { it.alias }) { alias ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onCallAlias(alias.alias) },
                            enabled = dialerRoleHeld && callPermissionGranted,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("☎  ${alias.alias}")
                        }
                        OutlinedButton(
                            onClick = { onMockCallAlias(alias.alias) },
                            enabled = state.activeCall == null
                        ) {
                            Text("Mock")
                        }
                    }
                }
            }
        }

        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
