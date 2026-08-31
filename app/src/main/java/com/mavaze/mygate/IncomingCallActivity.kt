package com.mavaze.mygate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mavaze.mygate.telephony.IncomingCallManager

class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            val incoming by
            IncomingCallManager.state.collectAsState()

            /*
             * Take a stable snapshot for this composition.
             *
             * This avoids Kotlin's smart-cast problem with the
             * delegated StateFlow property.
             */
            val incomingState = incoming

            LaunchedEffect(incomingState) {
                if (incomingState == null) {
                    finish()
                }
            }

            if (incomingState == null) {
                return@setContent
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {
                Text(
                    if (incomingState.incoming)
                        "Incoming call"
                    else
                        "Phone call",
                    style =
                        MaterialTheme.typography.headlineMedium
                )

                Spacer(
                    Modifier.height(24.dp)
                )

                Text(
                    incomingState.alias
                        .ifBlank { "Unknown resident" },
                    style =
                        MaterialTheme.typography.headlineLarge
                )

                incomingState.displayName?.let {
                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        it,
                        style =
                            MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(
                    Modifier.height(40.dp)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {

                    /*
                     * Active incoming call:
                     * the call has already been answered.
                     */
                    if (
                        incomingState.incoming &&
                        incomingState.active
                    ) {
                        OutlinedButton(
                            onClick = {
                                IncomingCallManager.reject()
                                finish()
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text("End")
                        }

                        /*
                         * Ringing incoming call.
                         */
                    } else if (
                        incomingState.incoming
                    ) {
                        OutlinedButton(
                            onClick = {
                                IncomingCallManager.reject()
                                finish()
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text("Decline")
                        }

                        Button(
                            onClick = {
                                IncomingCallManager.answer()
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text("Answer")
                        }

                        /*
                         * Outgoing call.
                         */
                    } else {
                        Button(
                            onClick = {
                                IncomingCallManager.reject()
                                finish()
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text("End call")
                        }
                    }
                }
            }
        }
    }
}