package com.mavaze.mygate.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mavaze.mygate.data.local.Society

@Composable
fun DefaultAdminScreen(
    state: DefaultAdminUiState,
    onCreateSociety: (String, String) -> Unit,
    onSetEnabled: (Long, Boolean) -> Unit,
    onRenameSociety: (Long, String) -> Unit,
    onDeleteSociety: (Long, String) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var societyName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }

    var renameSociety by remember { mutableStateOf<Society?>(null) }
    var deleteSociety by remember { mutableStateOf<Society?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            "My Gate",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Default Administrator",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { showCreate = !showCreate },
            enabled = !state.loading
        ) {
            Text(if (showCreate) "Cancel" else "Create Society")
        }

        if (showCreate) {
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = societyName,
                onValueChange = { societyName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Society name") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = adminEmail,
                onValueChange = { adminEmail = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Society Gmail address") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    onCreateSociety(
                        societyName,
                        adminEmail
                    )
                    societyName = ""
                    adminEmail = ""
                    showCreate = false
                },
                enabled =
                    societyName.isNotBlank() &&
                    adminEmail.isNotBlank() &&
                    !state.loading
            ) {
                Text("Save Society")
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Societies",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            items(
                state.societies,
                key = { it.id }
            ) { society ->

                Card(
                    Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp)
                    ) {
                        Text(
                            society.name,
                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )

                        Text(society.adminEmail)

                        Spacer(Modifier.height(8.dp))

                        Text(
                            if (society.enabled)
                                "Enabled"
                            else
                                "Disabled"
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSetEnabled(
                                        society.id,
                                        !society.enabled
                                    )
                                }
                            ) {
                                Text(
                                    if (society.enabled)
                                        "Disable"
                                    else
                                        "Enable"
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    renameSociety = society
                                }
                            ) {
                                Text("Rename")
                            }

                            OutlinedButton(
                                onClick = {
                                    deleteSociety = society
                                }
                            ) {
                                Text("Delete")
                            }
                        }
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

    renameSociety?.let { society ->
        var name by remember(society.id) {
            mutableStateOf(society.name)
        }

        AlertDialog(
            onDismissRequest = {
                renameSociety = null
            },
            title = { Text("Rename society") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Society name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameSociety(
                            society.id,
                            name
                        )
                        renameSociety = null
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        renameSociety = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteSociety?.let { society ->
        var email by remember(society.id) {
            mutableStateOf("")
        }

        AlertDialog(
            onDismissRequest = {
                deleteSociety = null
            },
            title = {
                Text("Delete ${society.name}?")
            },
            text = {
                Column {
                    Text(
                        "This deletes the society and its local " +
                            "watchman accounts from this device. " +
                            "Type the society Gmail address to confirm."
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Society Gmail address") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSociety(
                            society.id,
                            email
                        )
                        deleteSociety = null
                    },
                    enabled =
                        email.trim().equals(
                            society.adminEmail,
                            ignoreCase = true
                        )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        deleteSociety = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
