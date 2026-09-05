package com.mavaze.mygate.ui.watchman

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mavaze.mygate.data.local.User

@Composable
fun WatchmanScreen(user: User, state: WatchmanUiState, onCancelCall:()->Unit, onSkipCall:()->Unit, onHome:()->Unit) {
    val call = state.activeCall
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Text(user.displayName.ifBlank { "Watchman" }, style=MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick=onHome){Icon(Icons.Default.Home,null);Spacer(Modifier.width(4.dp));Text("Home")}
        }
        if(call != null){
            Spacer(Modifier.height(24.dp)); Text(call.alias,style=MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(8.dp));
            Card(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(call.contactName,style=MaterialTheme.typography.titleLarge);Text("Member ${call.attemptNumber} of ${call.totalContacts}");Text(if(state.connected)"Connected" else "Calling…");Text("${call.elapsedSeconds}s");Spacer(Modifier.height(16.dp));Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){OutlinedButton(onClick=onCancelCall){Icon(Icons.Default.Cancel,null);Text("Cancel")};Button(onClick=onSkipCall){Icon(Icons.Default.SkipNext,null);Text("Skip")}}}}
        }
        if(state.history.isNotEmpty()){
            Spacer(Modifier.height(20.dp));Text("Current sequence",style=MaterialTheme.typography.titleLarge);LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(state.history){h->Text("${h.contactName} • ${h.reason}")}}
        }
        state.error?.let{Spacer(Modifier.height(12.dp));Text(it,color=MaterialTheme.colorScheme.error)}
    }
}
