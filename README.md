# My Gate

My Gate is a local-first Android application for a society gate/watchman phone.

## Current architecture

- One generic APK can be installed on normal phones and the dedicated watchman phone.
- Local Room database stores the device's society/user cache.
- Default Administrator creates, enables/disables, renames and deletes societies.
- Society Administrator is represented by the society's single Gmail address.
- Watchmen are local users with temporary passwords and mandatory first-login password change.
- Society Gmail authentication uses Credential Manager / Sign in with Google.
- Google Drive and Google Contacts authorization is requested separately from identity authentication.
- The Society Administrator cannot enter the application until both required Google permissions are granted.
- The society Gmail account is the cross-device identity.
- The first device creates the society's canonical `MyGate-society.json` file in that Gmail account's Drive.
- Other devices can discover an already-created society by authenticating the Gmail address and reading that metadata.
- Room is the local cache; Drive is the temporary cross-device canonical store until a backend is introduced.
- Contacts are read through Google People API and synchronized into the local gate-contact table.
- Google access tokens are kept only in memory. They are deliberately not stored in Room or preferences.
- A society rename made locally is marked dirty and is pushed to Drive on the next authorized society-Gmail synchronization.
- The dedicated watchman phone has a kiosk/device-owner path. Once provisioned as a device owner and configured by a Society Administrator, My Gate can enter Android lock-task mode.
- The kiosk phone can launch My Gate automatically after boot.

## Google Cloud prerequisites

The Google Cloud project used by `server_client_id` must have the required APIs enabled and the OAuth consent configuration must permit the test Gmail account.

Required data scopes:

- `https://www.googleapis.com/auth/drive.file`
- `https://www.googleapis.com/auth/contacts.readonly`

The application also uses the Google Sign in ID-token flow for identity.

For production, Google OAuth consent-screen verification and the relevant API/user-data policies must be completed before distributing the application broadly.

## Important security design

The app never treats a Google ID token as a Drive/Contacts access token.

Identity authentication and Google-data authorization are two separate steps:

1. Authenticate the registered Gmail account.
2. Explicitly request Drive and Contacts authorization.
3. Verify that both scopes were granted.
4. Synchronize society metadata and contacts.
5. Only then establish the Society Administrator session.

Access tokens are short-lived and remain in memory only. A future backend can use Google's server-side offline authorization flow instead of moving refresh tokens into the APK.

## Development database

The Room database version is currently 5 and uses destructive migration because this is still development-stage software. Before production, replace this with explicit Room migrations.

## Kiosk provisioning

The APK contains a DeviceAdminReceiver and lock-task support. Android device-owner provisioning is an installation/device-management step and is not performed automatically by the application.

After the phone has been provisioned as a My Gate device owner:

1. Install My Gate.
2. Log in as the Society Administrator.
3. Configure the phone as the dedicated Watchman phone.
4. The app records kiosk mode for the configured society.
5. On subsequent Watchman launches/reboots, My Gate enters lock-task mode.

Do not enable kiosk mode on a development phone unless you intend to use it as the dedicated device.

## Current scope

The foundation is intentionally local-first so a future backend can replace the Drive synchronization layer without replacing the authentication, Room, role, or UI architecture.

## My Gate resident contact routing

Resident gate contacts are synchronized from the Google Contacts label `Resident`.
The Android app reads the following Google Contact custom fields:

- `MyGateAlias` — flat/alias shown to the watchman, e.g. `D9-301`
- `MyGatePriority` — numeric call priority, e.g. `10`, `20`, `30`

Contacts are grouped by alias and called in ascending priority. Phone numbers remain
internal to the call layer. During an active sequence the watchman sees the resident
name and alias and can **Cancel** (end the sequence) or **Skip** (disconnect and call
the next priority).

For disconnect/skip control, the device must make My Gate the Android default phone
app and grant `CALL_PHONE` permission.
