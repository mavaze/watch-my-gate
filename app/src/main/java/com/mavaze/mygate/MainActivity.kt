package com.mavaze.mygate

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.mavaze.mygate.auth.*
import com.mavaze.mygate.data.local.*
import com.mavaze.mygate.data.repository.*
import com.mavaze.mygate.ui.admin.*
import com.mavaze.mygate.ui.login.*
import com.mavaze.mygate.ui.watchman.WatchmanScreen
import com.mavaze.mygate.ui.watchman.WatchmanViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var societyRepository: SocietyRepository
    private lateinit var googleAuthRepository: GoogleAuthRepository
    private lateinit var googleAuthorizationRepository:
        GoogleAuthorizationRepository
    private lateinit var googleDataRepository:
        GoogleDataRepository
    private lateinit var appConfigRepository:
        AppConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database =
            (application as MyGateApplication).database

        userRepository =
            UserRepository(database.userDao())

        authRepository =
            AuthRepository(userRepository)

        societyRepository =
            SocietyRepository(database)

        googleAuthRepository =
            GoogleAuthRepository(applicationContext)

        googleAuthorizationRepository =
            GoogleAuthorizationRepository(applicationContext)

        googleDataRepository =
            GoogleDataRepository()

        appConfigRepository =
            AppConfigRepository(
                database.appConfigDao()
            )

        setContent {
            var dialerRoleHeld by remember {
                mutableStateOf(isDialerRoleHeld())
            }

            val dialerRoleLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    dialerRoleHeld = isDialerRoleHeld()
                }

            var callPermissionGranted by remember {
                mutableStateOf(
                    checkSelfPermission(
                        Manifest.permission.CALL_PHONE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                )
            }

            val callPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    callPermissionGranted = granted
                }

            val loginFactory = remember {
                LoginViewModelFactory(
                    authRepository,
                    googleAuthRepository
                )
            }

            val loginViewModel: LoginViewModel =
                viewModel(factory = loginFactory)

            val loginState by
            loginViewModel.uiState
                .collectAsStateWithLifecycle()

            val adminFactory = remember {
                DefaultAdminViewModelFactory(
                    societyRepository
                )
            }

            val adminViewModel:
                    DefaultAdminViewModel =
                viewModel(factory = adminFactory)

            val adminState by
            adminViewModel.state
                .collectAsStateWithLifecycle()

            var showGoogleConsent by
                remember { mutableStateOf(false) }

            val authorizationLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts
                        .StartIntentSenderForResult()
                ) { activityResult ->

                    if (
                        activityResult.resultCode !=
                        Activity.RESULT_OK
                    ) {
                        loginViewModel.authorizationFailed(
                            "Google access was not granted. " +
                                "Society administrator access remains blocked."
                        )
                        return@rememberLauncherForActivityResult
                    }

                    try {
                        val result =
                            googleAuthorizationRepository
                                .resultFromIntent(
                                    activityResult.data
                                )

                        lifecycleScope.launch {
                            processAuthorizationResult(
                                result,
                                loginViewModel
                            )
                        }
                    } catch (e: Exception) {
                        loginViewModel.authorizationFailed(
                            e.message
                                ?: "Unable to complete Google authorization"
                        )
                    }
                }

            fun requestGoogleAuthorization() {
                showGoogleConsent = false

                try {
                    googleAuthorizationRepository
                        .authorize()
                        .addOnSuccessListener { result ->

                            if (result.hasResolution()) {
                                val pendingIntent =
                                    result.pendingIntent

                                if (pendingIntent == null) {
                                    loginViewModel
                                        .authorizationFailed(
                                            "Google authorization requires a user action."
                                        )
                                    return@addOnSuccessListener
                                }

                                authorizationLauncher.launch(
                                    IntentSenderRequest.Builder(
                                        pendingIntent.intentSender
                                    ).build()
                                )
                            } else {
                                lifecycleScope.launch {
                                    processAuthorizationResult(
                                        result,
                                        loginViewModel
                                    )
                                }
                            }
                        }
                        .addOnFailureListener { error ->
                            loginViewModel.authorizationFailed(
                                error.message
                                    ?: "Unable to request Google access"
                            )
                        }
                } catch (e: Exception) {
                    loginViewModel.authorizationFailed(
                        e.message
                            ?: "Unable to request Google access"
                    )
                }
            }

            when {
                loginState.stage !=
                        LoginStage.LOGGED_IN -> {

                    MyGateLoginScreen(
                        state = loginState,
                        onUsernameChanged =
                            loginViewModel::setUsername,
                        onUsernameContinue =
                            loginViewModel::continueWithUsername,
                        onPasswordLogin =
                            loginViewModel::login,
                        onChangePassword =
                            loginViewModel::changePassword,
                        onGoogleLogin = {
                            if (
                                loginState.stage ==
                                LoginStage.GOOGLE_AUTH
                            ) {
                                loginViewModel.googleLogin()
                            } else if (
                                loginState.stage ==
                                LoginStage.GOOGLE_CONSENT
                            ) {
                                if (
                                    loginState.user
                                        ?.googleAuthorized == true
                                ) {
                                    requestGoogleAuthorization()
                                } else {
                                    showGoogleConsent = true
                                }
                            }
                        }
                    )

                    if (showGoogleConsent) {
                        AlertDialog(
                            onDismissRequest = {
                                showGoogleConsent = false
                            },
                            title = {
                                Text("Authorize My Gate")
                            },
                            text = {
                                Text(
                                    "My Gate needs access to the " +
                                        "registered society Gmail account's " +
                                        "Google Drive and Contacts. " +
                                        "Drive is used for shared society " +
                                        "metadata and future visitor files; " +
                                        "Contacts are used to synchronize " +
                                        "society contacts to the gate phone.\n\n" +
                                        "If you do not grant both permissions, " +
                                        "the Society Administrator account " +
                                        "will not be allowed into My Gate."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        requestGoogleAuthorization()
                                    }
                                ) {
                                    Text("Grant access")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = {
                                        showGoogleConsent = false
                                    }
                                ) {
                                    Text("Not now")
                                }
                            }
                        )
                    }
                }

                loginState.user?.role ==
                        UserRole.DEFAULT_ADMIN -> {

                    LaunchedEffect(
                        loginState.user?.id
                    ) {
                        adminViewModel.loadSocieties()
                    }

                    DefaultAdminScreen(
                        state = adminState,
                        onCreateSociety = {
                                name, email ->
                            adminViewModel.createSociety(
                                name,
                                email
                            )
                        },
                        onSetEnabled = {
                                societyId, enabled ->
                            adminViewModel.setEnabled(
                                societyId,
                                enabled
                            )
                        },
                        onRenameSociety = {
                                societyId, name ->
                            adminViewModel.renameSociety(
                                societyId,
                                name
                            )
                        },
                        onDeleteSociety = {
                                societyId, email ->
                            adminViewModel.deleteSociety(
                                societyId,
                                email
                            )
                        }
                    )
                }

                loginState.user?.role ==
                        UserRole.SOCIETY_ADMIN -> {

                    val societyId =
                        loginState.user?.societyId

                    if (societyId == null) {
                        ErrorScreen(
                            "Society information is missing."
                        )
                    } else {
                        val societyAdminFactory =
                            remember(societyId) {
                                SocietyAdminViewModelFactory(
                                    societyRepository,
                                    societyId
                                )
                            }

                        val societyAdminViewModel:
                                SocietyAdminViewModel =
                            viewModel(
                                factory =
                                    societyAdminFactory
                            )

                        val societyAdminState by
                        societyAdminViewModel.state
                            .collectAsStateWithLifecycle()

                        LaunchedEffect(societyId) {
                            societyAdminViewModel.load()
                        }

                        SocietyAdminScreen(
                            state = societyAdminState,
                            onCreateWatchman = {
                                    username,
                                    displayName,
                                    temporaryPassword ->
                                societyAdminViewModel
                                    .createWatchman(
                                        username,
                                        displayName,
                                        temporaryPassword
                                    )
                            },
                            onRenameSociety = {
                                    name ->
                                societyAdminViewModel
                                    .renameSociety(name)
                            },
                            onConfigureKiosk = {
                                lifecycleScope.launch {
                                    appConfigRepository
                                        .configureKiosk(
                                            societyId
                                        )

                                    if (
                                        KioskManager(
                                            this@MainActivity
                                        ).enter(
                                            this@MainActivity
                                        )
                                    ) {
                                        societyAdminViewModel
                                            .load()
                                    } else {
                                        societyAdminViewModel
                                            .setError(
                                                "This phone is not provisioned as a My Gate device owner. Configure Android device-owner/kiosk provisioning first."
                                            )
                                    }
                                }
                            }
                        )
                    }
                }

                else -> {
                    loginState.user?.let { user ->
                        if (user.role == UserRole.WATCHMAN) {
                            LaunchedEffect(user.id) {
                                val config =
                                    appConfigRepository.get()
                                if (
                                    config.deviceMode == "KIOSK" &&
                                    config.configuredSocietyId ==
                                    user.societyId
                                ) {
                                    KioskManager(
                                        this@MainActivity
                                    ).enter(
                                        this@MainActivity
                                    )
                                }
                            }
                            val watchmanFactory =
                                remember(user.societyId) {
                                    WatchmanViewModelFactory(
                                        user.societyId
                                            ?: -1L,
                                        database.gateContactDao(),
                                        applicationContext
                                    )
                                }

                            val watchmanViewModel:
                                WatchmanViewModel =
                                viewModel(
                                    factory = watchmanFactory
                                )

                            val watchmanState by
                                watchmanViewModel.state
                                    .collectAsStateWithLifecycle()

                            WatchmanScreen(
                                user = user,
                                state = watchmanState,
                                dialerRoleHeld = dialerRoleHeld,
                                onCallAlias =
                                    watchmanViewModel::callAlias,
                                onMockCallAlias =
                                    watchmanViewModel::mockCallAlias,
                                onCancelCall =
                                    watchmanViewModel::cancelCall,
                                onSkipCall =
                                    watchmanViewModel::skipCall,
                                onRequestDialerRole = {
                                    requestDialerRole(
                                        dialerRoleLauncher
                                    )
                                },
                                callPermissionGranted =
                                    callPermissionGranted,
                                onRequestCallPermission = {
                                    callPermissionLauncher.launch(
                                        Manifest.permission.CALL_PHONE
                                    )
                                }
                            )
                        } else {
                            ErrorScreen(
                                "Unsupported user role."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isDialerRoleHeld(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }

        val roleManager =
            getSystemService(RoleManager::class.java)
                ?: return false

        return roleManager.isRoleHeld(
            RoleManager.ROLE_DIALER
        )
    }

    private fun requestDialerRole(
        launcher:
            androidx.activity.result.ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        val roleManager =
            getSystemService(RoleManager::class.java)

        if (
            roleManager != null &&
            roleManager.isRoleAvailable(
                RoleManager.ROLE_DIALER
            ) &&
            !roleManager.isRoleHeld(
                RoleManager.ROLE_DIALER
            )
        ) {
            launcher.launch(
                roleManager.createRequestRoleIntent(
                    RoleManager.ROLE_DIALER
                )
            )
        }
    }

    private suspend fun processAuthorizationResult(
        result: AuthorizationResult,
        loginViewModel: LoginViewModel
    ) {
        val token = result.accessToken

        if (token.isNullOrBlank()) {
            loginViewModel.authorizationFailed(
                "Google did not return an access token."
            )
            return
        }

        val granted = result.grantedScopes.toSet()
        val required =
            GoogleScopes.required
                .map { it.scopeUri }
                .toSet()

        if (!granted.containsAll(required)) {
            loginViewModel.authorizationFailed(
                "Both Google Drive and Contacts permissions are required."
            )
            return
        }

        val state =
            loginViewModel.uiState.value

        val expectedEmail =
            (state.user?.username ?: state.username)
                .trim()
                .lowercase()

        GoogleDataSession.set(
            state.user?.societyId ?: -1L,
            token
        )

        /*
         * A Gmail account can be used on another phone even when
         * that phone has never had the society created locally.
         * In that case the canonical Drive metadata is the discovery
         * source for the local Room cache.
         */
        var user = state.user
        var societyId = user?.societyId

        if (user == null) {
            val discovered =
                googleDataRepository.discoverSociety(
                    token,
                    expectedEmail
                )

            val cloudName =
                discovered.getOrElse {
                    loginViewModel.authorizationFailed(
                        "Unable to discover the society in Google Drive: " +
                            (it.message ?: "unknown error")
                    )
                    return
                }

            if (cloudName.isNullOrBlank()) {
                loginViewModel.authorizationFailed(
                    "This Gmail account is not registered with a My Gate society yet."
                )
                return
            }

            try {
                societyId =
                    societyRepository.createSociety(
                        cloudName,
                        expectedEmail
                    )

                user =
                    authRepository.findUser(
                        expectedEmail
                    )
            } catch (e: Exception) {
                user =
                    authRepository.findUser(
                        expectedEmail
                    )
            }

            if (user == null || societyId == null) {
                loginViewModel.authorizationFailed(
                    "Unable to create the local society configuration."
                )
                return
            }

            loginViewModel.adoptDiscoveredUser(user)
        }

        if (
            user == null ||
            societyId == null
        ) {
            loginViewModel.authorizationFailed(
                "Society information is missing."
            )
            return
        }

        val society =
            societyRepository.findById(societyId)

        if (society == null) {
            loginViewModel.authorizationFailed(
                "Society no longer exists on this device."
            )
            return
        }

        GoogleDataSession.set(
            society.id,
            token
        )

        val syncResult =
            if (society.cloudMetadataDirty) {
                googleDataRepository.updateSocietyMetadata(
                    token,
                    society.name,
                    society.adminEmail,
                    society.logoPath
                ).fold(
                    onSuccess = {
                        societyRepository
                            .markCloudMetadataSynced(
                                society.id
                            )

                        googleDataRepository
                            .synchronizeSociety(
                                token,
                                society.name,
                                society.adminEmail
                            )
                    },
                    onFailure = {
                        Result.failure(it)
                    }
                )
            } else {
                googleDataRepository.synchronizeSociety(
                    token,
                    society.name,
                    society.adminEmail
                )
            }

        syncResult
            .onSuccess { synced ->
                if (
                    !society.cloudMetadataDirty &&
                    synced.societyName.isNotBlank()
                ) {
                    societyRepository.applyCloudMetadata(
                        society.id,
                        synced.societyName
                    )
                }

                syncContacts(
                    society.id,
                    synced.contacts
                )

                loginViewModel.authorizationSucceeded(
                    user
                )
            }
            .onFailure { error ->
                GoogleDataSession.clear()

                loginViewModel.authorizationFailed(
                    "Google access was granted, but My Gate " +
                        "could not synchronize society data: " +
                            (error.message ?: "${error.javaClass.simpleName}: $error")
                )
            }
    }

    private suspend fun syncContacts(
        societyId: Long,
        contacts: List<com.mavaze.mygate.data.repository.GoogleContact>
    ) {
        val dao =
            database.gateContactDao()

        Log.d(
            "MyGateContacts",
            "Persisting ${contacts.size} Google resident contacts for societyId=$societyId"
        )

        val entities =
            contacts.map { contact ->
                GateContact(
                    societyId = societyId,
                    googleResourceName =
                        contact.resourceName,
                    displayName =
                        contact.displayName,
                    phoneNumbersJson =
                        JSONArray(
                            contact.phoneNumbers
                        ).toString(),
                    alias =
                        contact.alias
                            ?.trim()
                            ?.takeIf { it.isNotBlank() },
                    priority =
                        contact.priority
                )
            }

        dao.replaceForSociety(
            societyId,
            entities
        )

        Log.d(
            "MyGateContacts",
            "Room resident replacement complete: inserted=${entities.size}"
        )
    }

}

class LoginViewModelFactory(
    private val authRepository: AuthRepository,
    private val googleAuthRepository: GoogleAuthRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                LoginViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(
                authRepository,
                googleAuthRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel"
        )
    }
}

class WatchmanViewModelFactory(
    private val societyId: Long,
    private val dao: GateContactDao,
    private val context: android.content.Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                WatchmanViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return WatchmanViewModel(
                societyId,
                dao,
                context.applicationContext
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel"
        )
    }
}

class DefaultAdminViewModelFactory(
    private val repository: SocietyRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                DefaultAdminViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return DefaultAdminViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel"
        )
    }
}

class SocietyAdminViewModelFactory(
    private val repository: SocietyRepository,
    private val societyId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                SocietyAdminViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return SocietyAdminViewModel(
                repository,
                societyId
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel"
        )
    }
}

@Composable
private fun MyGateLoginScreen(
    state: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onUsernameContinue: () -> Unit,
    onPasswordLogin: (String) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onGoogleLogin: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                "My Gate",
                style =
                    MaterialTheme.typography.headlineLarge
            )

            Spacer(Modifier.height(32.dp))

            when (state.stage) {
                LoginStage.USERNAME -> {
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = onUsernameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onUsernameContinue,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue")
                    }
                }

                LoginStage.LOCAL_PASSWORD -> {
                    Text(
                        state.user?.displayName ?: state.username,
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        visualTransformation =
                            PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onPasswordLogin(password)
                        },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Login")
                    }
                }

                LoginStage.CHANGE_PASSWORD -> {
                    Text(
                        "Change Password",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Your temporary password must be changed before continuing."
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New password") },
                        visualTransformation =
                            PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        visualTransformation =
                            PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onChangePassword(
                                newPassword,
                                confirmPassword
                            )
                        },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Password")
                    }
                }

                LoginStage.GOOGLE_AUTH -> {
                    Text(
                        "Society Administrator",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Sign in with the registered society Gmail account."
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onGoogleLogin,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue with Google")
                    }
                }

                LoginStage.GOOGLE_CONSENT -> {
                    Text(
                        "Google access required",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        if (state.user?.googleAuthorized == true)
                            "Continue to refresh access to the society's Google Drive and Contacts."
                        else
                            "My Gate needs the registered society Gmail's Drive and Contacts access before this account can enter the app."
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onGoogleLogin,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.user?.googleAuthorized == true)
                                "Continue"
                            else
                                "Grant Google access"
                        )
                    }
                }

                LoginStage.LOGGED_IN -> {
                    Text(
                        "Welcome",
                        style =
                            MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(state.user?.displayName ?: "")
                }
            }

            if (state.busy) {
                Spacer(Modifier.height(16.dp))
                Text("Please wait…")
            }

            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement =
                Arrangement.Center,
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
