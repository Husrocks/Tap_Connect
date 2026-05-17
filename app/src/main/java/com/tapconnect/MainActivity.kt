package com.tapconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tapconnect.ui.navigation.AppNavigation
import com.tapconnect.ui.theme.TapConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TapConnectTheme {
                AppNavigation()
            }
        }
    }
}
