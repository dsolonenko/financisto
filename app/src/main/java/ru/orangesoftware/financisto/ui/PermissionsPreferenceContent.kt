package ru.orangesoftware.financisto.ui

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.core.annotation.KoinExperimentalAPI
import ru.orangesoftware.financisto.R
import ru.orangesoftware.financisto.app.DependenciesHolder
import ru.orangesoftware.financisto.persistance.PreferencesStore

@OptIn(KoinExperimentalAPI::class)
@Composable
fun PermissionsPreferenceContent() {
//    val preferencesStore = PreferencesStore(LocalContext.current)

    MaterialTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colorScheme.background) {
            KoinAndroidContext {
                Content()
            }
        }
    }
}

@Composable
private fun Content(preferencesStore: PreferencesStore = DependenciesHolder().preferencesStore/*koinInject()*/) {
    val scrollableState = rememberScrollableState {
        it
    }
    Column(
        modifier = Modifier
            .scrollable(
                state = scrollableState,
                orientation = Orientation.Vertical,
            )
            .padding(10.dp)
    ) {
        Title()
        Spacer(modifier = Modifier.size(10.dp))
        Description()
        Spacer(modifier = Modifier.size(10.dp))
        Storage(preferencesStore)
        Contacts()
        Camera()
        Sms()
    }
}

@Composable
private fun Title() {
    Text(
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Normal
        ),
        text = stringResource(R.string.request_permissions_title)
    )
}

@Composable
private fun Description() {
    Text(
        style = MaterialTheme.typography.bodyMedium,
        text = stringResource(R.string.request_permissions_description)
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Storage(preferencesStore: PreferencesStore) {
    val context = LocalContext.current

    val latestFolder = preferencesStore.backupPreferencesRx.blockingFirst()//backupPreferencesFlow.collectAsState(initial = null)
    var permissionGranted = latestFolder.let {
        context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == it.folder
        }
    } ?: false
    val pickFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Update the state with the Uri
            permissionGranted = true
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            Toast.makeText(context, "Permission granted: $uri", Toast.LENGTH_LONG).show()
            Log.d("PermissionsPreferenceContent", "Permission granted: $uri")
            CoroutineScope(Dispatchers.IO).launch {
                preferencesStore.updateBackupFolderUri(uri)
            }
        } else {
            permissionGranted = false
        }
    }
    PermissionBlock(
        title = stringResource(R.string.request_permissions_storage_title),
        description = stringResource(R.string.request_permissions_storage_description),
        isSwitchChecked = permissionGranted,//storagePermissionState.status.isGranted,
        isSwitchDisabled = permissionGranted,//storagePermissionState.status.isGranted,
//        isSwitchDisabled = storagePermissionState.status.isGranted || (!storagePermissionState.status.isGranted && !storagePermissionState.status.shouldShowRationale),
        onSwitch = {
//            storagePermissionState.launchPermissionRequest()

// In your button's click
            pickFolderLauncher.launch(latestFolder.folder)
        },
//        onSwitch = { if (storagePermissionState.status.shouldShowRationale) {
//            Toast.makeText(context, "Show rationale", Toast.LENGTH_LONG).show()
//        } else {
//            storagePermissionState.launchPermissionRequest()
//        } },
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Contacts() {
    val contactsPermissionState = rememberPermissionState(
        android.Manifest.permission.GET_ACCOUNTS
    )
    PermissionBlock(
        title = stringResource(R.string.request_permissions_get_accounts_title),
        description = stringResource(R.string.request_permissions_get_accounts_description),
        isSwitchChecked = contactsPermissionState.status.isGranted,
        isSwitchDisabled = contactsPermissionState.status.isGranted,
        onSwitch = { contactsPermissionState.launchPermissionRequest() },
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Camera() {
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    PermissionBlock(
        title = stringResource(R.string.request_permissions_camera_title),
        description = stringResource(R.string.request_permissions_camera_description),
        isSwitchChecked = cameraPermissionState.status.isGranted,
        isSwitchDisabled = cameraPermissionState.status.isGranted,
        onSwitch = { cameraPermissionState.launchPermissionRequest() },
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Sms() {
    val smsPermissionState = rememberPermissionState(
        android.Manifest.permission.RECEIVE_SMS
    )
    PermissionBlock(
        title = stringResource(R.string.request_permissions_receive_sms_title),
        description = stringResource(R.string.request_permissions_receive_sms_description),
        isSwitchChecked = smsPermissionState.status.isGranted,
        isSwitchDisabled = smsPermissionState.status.isGranted,
        onSwitch = { smsPermissionState.launchPermissionRequest() },
    )
}

@Composable
private fun PermissionBlock(
    title: String,
    description: String,
    isSwitchChecked: Boolean,
    isSwitchDisabled: Boolean,
    onSwitch: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.padding(10.dp)) {
        Column(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start)
                .weight(weight = 1f, fill = true),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Normal
                ),
                text = title
            )
            Text(
                style = MaterialTheme.typography.bodyMedium,
                text = description
            )
        }
        Switch(
            checked = isSwitchChecked,
            enabled = !isSwitchDisabled,
            onCheckedChange = onSwitch,
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_7_PRO,
)
private fun Preview() {
    PermissionsPreferenceContent()
}
