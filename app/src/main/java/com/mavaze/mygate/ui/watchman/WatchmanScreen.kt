package com.mavaze.mygate.ui.watchman

import androidx.activity.compose.BackHandler
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
    onRequestCallPermission: () -> Unit,
    onLogout: () -> Unit,
    onHome: () -> Unit
) {

    /*
     * When a call sequence is active, Android Back means
     * "go back to Watchman Home".
     *
     * It does NOT log the Watchman out.
     */
    BackHandler(
        enabled =
            state.activeCall != null ||
                    state.history.isNotEmpty()
    ) {
        onHome()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
    ) {

        Text(
            "My Gate",
            style =
                MaterialTheme.typography.headlineLarge
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                "Watchman",
                style =
                    MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                /*
                 * Home is always available.
                 *
                 * On the resident home screen it simply has no
                 * visible effect because there is already nowhere
                 * further back in the Watchman workflow.
                 */
                OutlinedButton(
                    onClick = onHome
                ) {
                    Text("Home")
                }

                OutlinedButton(
                    onClick = onLogout
                ) {
                    Text("Logout")
                }
            }
        }

        Spacer(
            Modifier.height(24.dp)
        )

        /*
         * Phone setup remains above the resident workflow.
         */
        if (!dialerRoleHeld) {

            Card(
                Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(20.dp)
                ) {

                    Text(
                        "Phone calling needs My Gate to be the default phone app.",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        "Enable this when actual phone calling is ready."
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Button(
                        onClick =
                            onRequestDialerRole,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Enable phone calling"
                        )
                    }
                }
            }

        } else if (!callPermissionGranted) {

            Card(
                Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(20.dp)
                ) {

                    Text(
                        "My Gate needs phone permission to place and manage calls.",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Button(
                        onClick =
                            onRequestCallPermission,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Allow phone calls"
                        )
                    }
                }
            }
        }

        /*
         * --------------------------------------------------------
         * ACTIVE CALL / COMPLETED CALL SEQUENCE
         * --------------------------------------------------------
         */
        if (
            state.activeCall != null ||
            state.history.isNotEmpty()
        ) {

            val call =
                state.activeCall

            /*
             * The alias is shown exactly ONCE here.
             *
             * It is outside the call card and is not repeated
             * underneath the person being called.
             */
            val displayedAlias =
                call?.alias
                    ?: state.history
                        .firstOrNull()
                        ?.alias

            if (
                displayedAlias != null
            ) {

                Text(
                    displayedAlias,
                    style =
                        MaterialTheme.typography.headlineMedium
                )

                Spacer(
                    Modifier.height(16.dp)
                )
            }

            if (call != null) {

                Card(
                    Modifier.fillMaxWidth()
                ) {

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            "Calling",
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            call.contactName,
                            style =
                                MaterialTheme.typography.headlineSmall
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            "Person ${call.attemptNumber} of ${call.totalContacts}",
                            style =
                                MaterialTheme.typography.bodyMedium
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            "Elapsed: ${
                                formatDuration(
                                    call.elapsedSeconds
                                )
                            }",
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Spacer(
                            Modifier.height(20.dp)
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(16.dp)
                        ) {

                            OutlinedButton(
                                onClick =
                                    onCancelCall,
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick =
                                    onSkipCall,
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text("Skip")
                            }
                        }
                    }
                }
            }

            if (
                state.history.isNotEmpty()
            ) {

                Spacer(
                    Modifier.height(20.dp)
                )

                Text(
                    "Call history",
                    style =
                        MaterialTheme.typography.titleLarge
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(
                                1f,
                                fill = false
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        state.history,
                        key = {
                            "${it.alias}-${it.attemptNumber}-${it.contactName}"
                        }
                    ) { history ->

                        CallHistoryCard(
                            history
                        )
                    }
                }
            }

            /*
             * Once the sequence is over, Home is the clear way
             * back to the Watchman workflow home.
             */
            if (
                state.activeCall == null
            ) {

                Spacer(
                    Modifier.height(16.dp)
                )

                Button(
                    onClick = onHome,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("Home")
                }
            }

        } else {

            /*
             * ----------------------------------------------------
             * WATCHMAN HOME
             * ----------------------------------------------------
             */

            Text(
                "Residents",
                style =
                    MaterialTheme.typography.titleLarge
            )

            Spacer(
                Modifier.height(12.dp)
            )

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    state.aliases,
                    key = {
                        it.alias
                    }
                ) { alias ->

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Button(
                            onClick = {
                                onCallAlias(
                                    alias.alias
                                )
                            },
                            enabled =
                                dialerRoleHeld &&
                                        callPermissionGranted,
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                "☎  ${alias.alias}"
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onMockCallAlias(
                                    alias.alias
                                )
                            },
                            enabled =
                                state.activeCall == null
                        ) {

                            Text("Mock")
                        }
                    }
                }
            }
        }

        state.error?.let {

            Spacer(
                Modifier.height(16.dp)
            )

            Text(
                it,
                color =
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CallHistoryCard(
    history: CallHistoryEntry
) {

    Card(
        Modifier.fillMaxWidth()
    ) {

        Column(
            Modifier.padding(12.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    "${history.attemptNumber}. ${history.contactName}",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    formatDuration(
                        history.durationSeconds
                    ),
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                historyReasonLabel(
                    history.reason
                ),
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDuration(
    seconds: Long
): String {

    val minutes =
        seconds / 60

    val remaining =
        seconds % 60

    return if (minutes > 0) {

        "%d:%02d".format(
            minutes,
            remaining
        )

    } else {

        "${remaining}s"
    }
}

private fun historyReasonLabel(
    reason: String
): String =

    when (reason) {

        "busy" ->
            "Busy"

        "rejected" ->
            "Rejected"

        "unreachable" ->
            "Phone unreachable"

        "invalid_number" ->
            "Invalid number"

        "no_answer" ->
            "No answer"

        "completed" ->
            "Connected / completed"

        "cancelled" ->
            "Cancelled"

        "skipped" ->
            "Skipped"

        "mock" ->
            "Mock call"

        else ->
            reason
                .replace(
                    "_",
                    " "
                )
                .replaceFirstChar {
                    it.uppercase()
                }
    }