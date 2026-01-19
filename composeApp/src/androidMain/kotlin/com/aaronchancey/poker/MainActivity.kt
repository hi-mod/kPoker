package com.aaronchancey.poker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

// @Preview
// @Composable
// fun AppAndroidPreview() {
//    val settings = SharedPreferencesSettings(
//        ContextWrapper(null).getSharedPreferences("poker_prefs", MODE_PRIVATE),
//    )
//    App(settings = settings)
// }
