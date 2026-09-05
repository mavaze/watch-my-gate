package com.mavaze.mygate.ui.watchman

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mavaze.mygate.data.local.User

@Composable
fun CallSequenceScreen(
    user: User,
    state: WatchmanUiState,
    onCallAlias: (WatchmanAlias) -> Unit,
    onMockCallAlias: (WatchmanAlias) -> Unit,
    onCancelCall: () -> Unit,
    onSkipCall: () -> Unit,
    onRequestDialerRole: () -> Unit,
    dialerRoleHeld: Boolean,
    callPermissionGranted: Boolean,
    onRequestCallPermission: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val call = state.activeCall
        if (call != null) {
            Text(call.alias, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(call.contactName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Member ${call.attemptNumber} of ${call.totalContacts}")
            Spacer(Modifier.height(12.dp))
            Text(formatDuration(call.elapsedSeconds), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalButton(onClick = onCancelCall, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CallEnd, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text("Cancel")
                }
                Button(onClick = onSkipCall, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text("Skip")
                }
            }
        } else if (state.history.isNotEmpty()) {
            Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Call sequence complete", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Return to Residents to start another call sequence.")
        } else {
            Text("No active call sequence", style = MaterialTheme.typography.headlineSmall)
            if (!dialerRoleHeld) {
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRequestDialerRole) { Text("Enable phone calling") }
            } else if (!callPermissionGranted) {
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRequestCallPermission) { Text("Allow phone calls") }
            }
        }
        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "%02d:%02d".format(minutes, remaining)
}
