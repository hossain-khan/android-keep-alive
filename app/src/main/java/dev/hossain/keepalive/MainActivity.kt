package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import dev.hossain.keepalive.service.WatchdogService
import dev.hossain.keepalive.ui.theme.KeepALiveTheme

class MainActivity : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val TAG = "MainActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeepALiveTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        requestPermissionLauncher = this.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[POST_NOTIFICATIONS] == true) {
                // Permission granted, you can post notifications
                Log.d(TAG, "onCreate: POST_NOTIFICATIONS Permission granted")
            } else {
                // Permission denied, handle accordingly
                Log.d(TAG, "onCreate: POST_NOTIFICATIONS Permission denied")
            }

            if (permissions[PACKAGE_USAGE_STATS] == true) {
                // Permission granted, you can get package usage stats
                Log.d(TAG, "onCreate: PACKAGE_USAGE_STATS Permission granted")
            } else {
                // Permission denied, handle accordingly
                Log.d(TAG, "onCreate: PACKAGE_USAGE_STATS Permission denied")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if ((ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                    this,
                    PACKAGE_USAGE_STATS
                )
                        != PackageManager.PERMISSION_GRANTED)
            ) {
                requestPermissionLauncher.launch(arrayOf(POST_NOTIFICATIONS, PACKAGE_USAGE_STATS))
            }
        }

        if(hasUsageStatsPermission(this)) {
            Log.d(TAG, "onCreate: PACKAGE_USAGE_STATS Permission granted")
        } else {
            Log.d(TAG, "onCreate: PACKAGE_USAGE_STATS Permission denied")
            requestUsageStatsPermission()
        }
    }

    override fun onStart() {
        super.onStart()
        startWatchdogService()
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun startWatchdogService() {
        val serviceIntent = Intent(this, WatchdogService::class.java)
        startService(serviceIntent)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KeepALiveTheme {
        Greeting("Android")
    }
}