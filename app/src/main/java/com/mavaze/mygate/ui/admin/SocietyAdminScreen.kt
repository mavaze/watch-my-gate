package com.mavaze.mygate.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mavaze.mygate.data.local.User

@Composable
fun SocietyAdminScreen(
    state: SocietyAdminUiState,
    onCreateWatchman: (String, String, String) -> Unit,
    onRenameSociety: (String) -> Unit,
    onConfigureKiosk: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var temporaryPassword by remember { mutableStateOf("") }
    var newName by remember(state.societyName) {
        mutableStateOf(state.societyName)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            state.societyName,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(state.adminEmail)

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    showCreate = !showCreate
                }
            ) {
                Text(
                    if (showCreate)
                        "Cancel"
                    else
                        "Add Watchman"
                )
            }

            OutlinedButton(
                onClick = {
                    newName = state.societyName
                    showRename = true
                }
            ) {
                Text("Rename Society")
            }

            OutlinedButton(
                onClick = onConfigureKiosk
            ) {
                Text("Configure Watchman Phone")
            }
        }

        if (showCreate) {
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Watchman name") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = temporaryPassword,
                onValueChange = {
                    temporaryPassword = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Temporary password") },
                visualTransformation =
                    PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    onCreateWatchman(
                        username,
                        displayName,
                        temporaryPassword
                    )
                    username = ""
                    displayName = ""
                    temporaryPassword = ""
                    showCreate = false
                },
                enabled =
                    username.isNotBlank() &&
                    displayName.isNotBlank() &&
                    temporaryPassword.isNotBlank() &&
                    !state.loading
            ) {
                Text("Create Watchman")
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Watchmen",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            items(
                state.watchmen,
                key = { it.id }
            ) { watchman ->
                Card(
                    Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp)
                    ) {
                        Text(
                            watchman.displayName,
                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )
                        Text(watchman.username)
                        Text(
                            if (watchman.enabled)
                                "Enabled"
                            else
                                "Disabled"
                        )
                    }
                }
            }
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = {
                showRename = false
            },
            title = {
                Text("Rename society")
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Society name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameSociety(newName)
                        showRename = false
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRename = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
