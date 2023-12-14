package ru.orangesoftware.financisto.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import ru.orangesoftware.financisto.activity.RequestPermissionActivity.Constants.REQUESTED_PERMISSION
import ru.orangesoftware.financisto.ui.PermissionsPreferenceContent
import ru.orangesoftware.financisto.utils.MyPreferences

class RequestPermissionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PermissionsPreferenceContent()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(MyPreferences.switchLocale(newBase))
    }

    object Constants {
        const val REQUESTED_PERMISSION = "requestedPermission"
    }

    companion object {

        @JvmStatic
        fun intent(context: Context) = IntentBuilder(context)
    }

    class IntentBuilder(private val context: Context) {
        val intent: Intent = Intent(context, RequestPermissionActivity::class.java)

        fun requestedPermission(requestedPermission: String): IntentBuilder {
            intent.putExtra(REQUESTED_PERMISSION, requestedPermission)
            return this
        }

        fun start() {
            context.startActivity(intent)
        }
    }
}
