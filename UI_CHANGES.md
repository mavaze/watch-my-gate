# My Gate UI / Navigation Implementation

## Included
- Material 3 watchman application shell.
- System-bar-safe top app bar and bottom navigation.
- Top-level navigation: Home, Residents, Visitors, Tasks, History.
- Notification icon and watchman initial avatar.
- Profile menu with Logout; Logout is no longer a permanent Home-screen action.
- Standard Material icons for common navigation/actions.
- Residents list calls directly into Call Sequence; no duplicate resident-selection page.
- Existing calling ViewModel/controller callbacks are retained.
- Call Sequence uses focused Cancel and Skip controls with icons and labels.
- Native Android CallLog integration for resident incoming/outgoing history.
- Call history is matched to synced resident phone numbers and displays individual member names with aliases, sorted newest first.
- READ_CALL_LOG runtime permission flow added.
- Tasks top-level page and placeholder Add Task screen added. Google Calendar and alarms are intentionally deferred.
- Existing visitor/history placeholders remain ready for the next implementation phase.

## Intentionally not changed
- Google authentication/authorization.
- Google Contacts synchronization.
- Room resident data model.
- MyGateCallController and existing telephony event handling.
- Existing call sequence result/timeout logic.
- Visitor implementation and visitor history persistence.
- Google Calendar task synchronization and Android alarms.
