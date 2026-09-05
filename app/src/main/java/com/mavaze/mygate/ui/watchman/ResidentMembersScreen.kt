package com.mavaze.mygate.ui.watchman

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResidentMembersScreen(
    state: WatchmanUiState,
    onCallAlias: (WatchmanAlias) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Select a resident to call", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        if (state.aliases.isEmpty()) {
            Text("No resident members are available.", style = MaterialTheme.typography.bodyLarge)
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.aliases, key = { it.alias }) { alias ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(alias.alias, style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (alias.memberCount == 1) "1 member" else "${alias.memberCount} members",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        FilledTonalIconButton(onClick = { onCallAlias(alias) }) {
                            Icon(Icons.Default.Call, contentDescription = "Call ${alias.alias}")
                        }
                    }
                }
            }
        }
    }
}
